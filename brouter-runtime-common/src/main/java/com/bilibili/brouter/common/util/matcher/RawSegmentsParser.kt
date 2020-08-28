package com.bilibili.brouter.common.util.matcher


/**
 * TEXT: [unreserved char]+
 *
 * text: TEXT
 * segment_parts: '(' ('?<' text '>')? normal_segment ('|' normal_segment)* ')'
 * normal_segment: (text | segment_parts) normal_segment?
 * wildcard_segment: text? ('{' text? '}' | '*') text?
 * prefix_segment: '**'
 * segment: normal_segment | wildcard_segment | prefix_segment
 * segments: segment ('/' segment)*
 * scheme: segment
 * router_uri: (scheme '://' | '/')?  segments EOF
 * @author dieyi
 * Created at 2020/4/23.
 */

abstract class RawSegmentsParser {

    protected abstract fun defaultSchemeSegment(input: String): RawSegment

    fun parse(input: String): RawSegments = try {
        val lexer = BRouterUriLexer(input)

        val segments = ArrayList<RawSegment>(4)
        // check if start with '/'
        if (lexer.peekChar() == '/') {
            lexer.skipChar()
            segments += defaultSchemeSegment(input)
            segments += parseSegment(lexer) // host
        } else {
            val s = parseSegment(lexer) // first segment or scheme
            if (lexer.reachEnd() || lexer.peekChar() != ':') {
                segments += defaultSchemeSegment(input)
                segments += s
            } else {
                lexer.skipChar()
                lexer.match('/')
                lexer.match('/')
                segments += s
                segments += parseSegment(lexer) // host
            }
        }

        // parse rest segments
        while (true) {
            if (lexer.reachEnd()) break
            lexer.match('/')
            if (lexer.reachEnd()) break
            segments += parseSegment(lexer)
        }
        segments
    } catch (e: TokenMismatchException) {
        throw InvalidUriException("Error on pause $input.", e)
    }

    private fun parseSegment(lexer: BRouterUriLexer): RawSegment {
        val prefix = if (!lexer.isReservedChar()) {
            lexer.nextText()
        } else {
            null
        }
        return if (prefix != null && lexer.reachEnd()) {
            NormalRawSegment(prefix, null, null)
        } else {
            val c = lexer.peekChar()
            when (c) {
                '(' -> {
                    parseNormalSegment(lexer).let {
                        if (prefix == null) {
                            it
                        } else {
                            NormalRawSegment(prefix, null, it)
                        }
                    }
                }
                '{' -> {
                    lexer.skipChar()
                    val name = if (lexer.peekChar() == '}') {
                        lexer.skipChar()
                        null
                    } else {
                        lexer.nextText().also {
                            lexer.match('}')
                        }
                    }
                    val suffix = if (lexer.reachEnd() || lexer.isReservedChar()) {
                        null
                    } else {
                        lexer.nextText()
                    }
                    WildCardRawSegment(prefix, name, suffix)
                }
                '*' -> {
                    lexer.skipChar()
                    when {
                        lexer.reachEnd() -> {
                            WildCardRawSegment(prefix, null, null)
                        }
                        lexer.peekChar() == '*' -> {
                            lexer.skipChar()
                            PrefixRawSegment
                        }
                        else -> {
                            WildCardRawSegment(prefix, null, lexer.nextText())
                        }
                    }
                }
                else -> {
                    if (prefix == null) {
                        lexer.unexpectedChar("({*")
                    } else {
                        NormalRawSegment(prefix, null, null)
                    }
                }
            }
        }
    }

    private fun parseNormalSegment(
        lexer: BRouterUriLexer
    ): NormalRawSegment {
        var parts: SegmentParts? = null
        var text: Text? = null
        if (lexer.peekChar() == '(') {
            parts = parseSegmentParts(lexer)
        } else {
            text = lexer.nextText()
        }
        return NormalRawSegment(
            text,
            parts,
            if (!lexer.reachEnd() && (!lexer.isReservedChar() || lexer.peekChar() == '(')) {
                parseNormalSegment(lexer)
            } else {
                null
            }
        )
    }

    private fun parseSegmentParts(lexer: BRouterUriLexer): SegmentParts {
        lexer.match('(')
        val name = if (lexer.peekChar() == '?') {
            lexer.skipChar()
            lexer.match('<')
            lexer.nextText().also {
                lexer.match('>')
            }
        } else {
            null
        }
        val innerSegments = ArrayList<NormalRawSegment>(2)
        innerSegments += parseNormalSegment(lexer)
        while (lexer.peekChar() == '|') {
            lexer.skipChar()
            innerSegments += parseNormalSegment(lexer)
        }
        lexer.match(')')
        return SegmentParts(name, innerSegments)
    }

    companion object {

        operator fun invoke(defaultScheme: String): RawSegmentsParser {
            val lexer = BRouterUriLexer(defaultScheme)
            val defaultSchemeSegment = NormalRawSegment(lexer.nextText(), null, null)
            if (!lexer.reachEnd()) {
                throw InvalidUriException("Invalid default scheme '$defaultScheme'.")
            }
            return NoSchemeSegmentsParser(defaultSchemeSegment)
        }

        fun forceScheme() : RawSegmentsParser = ForSchemeSegmentsParser
    }
}

internal class NoSchemeSegmentsParser(private val defaultSchemeSegment: RawSegment) : RawSegmentsParser() {
    override fun defaultSchemeSegment(input: String): RawSegment = defaultSchemeSegment
}

object ForSchemeSegmentsParser : RawSegmentsParser() {
    override fun defaultSchemeSegment(input: String): RawSegment {
        throw InvalidUriException("Scheme required for '$input'.")
    }
}

internal class BRouterUriLexer(private val input: String) {
    private var position = 0
    private var mark = 0
    private val end = input.length

    fun match(c: Char) {
        val p = position++
        try {
            val r = input[p]
            if (c != r) {
                throw TokenMismatchException("Expected '$c' at $p but is '$r'.")
            }
        } catch (e: IndexOutOfBoundsException) { // reduce one check
            throw TokenMismatchException("Expected '$c' at $p but reach end.")
        }
    }

    fun reachEnd(): Boolean = position >= end

    fun mark() {
        mark = position
    }

    fun reset() {
        position = mark
    }

    fun nextChar(): Char = input[position++]
    fun skipChar() {
        position++
    }

    fun peekChar(): Char =
        try {
            input[position]
        } catch (e: IndexOutOfBoundsException) {
            throw TokenMismatchException("Expected a char at $position but reach end.")
        }

    fun nextText(): Text {
        var p = position
        while (p < end) {
            val c = input[p]
            if (isReservedChar(c)) {
                break
            }
            p++
        }
        if (p <= position) {
            throw TokenMismatchException("Expected normal text at $p but is empty.")
        }
        val start = position
        position = p
        return input.substring(start, p)
    }

    fun unexpectedChar(s: String): Nothing {
        throw TokenMismatchException("Expected one of \"$s\" but is '${peekChar()}' at $position.")
    }

    fun isReservedChar(): Boolean {
        return Companion.isReservedChar(input[position])
    }

    companion object {
        private val MAP: BooleanArray

        init {
            MAP = BooleanArray(128).apply {
                arrayOf('/', '(', ')', '{', '}', '<', '>', '?', ':', '|', '*').forEach {
                    this[it.toInt()] = true
                }
            }
        }

        @JvmStatic
        fun isReservedChar(c: Char) = c.toInt().let {
            it < 128 && MAP[it]
        }
    }
}

typealias Text = String

sealed class RawSegment

data class SegmentParts(val name: Text?, val innerSegments: List<NormalRawSegment>) {
    override fun toString(): String {
        return "(${name?.let { "?<$it>" } ?: ""}${innerSegments.joinToString("|")})"
    }
}

data class NormalRawSegment(
    val text: Text?,
    val parts: SegmentParts?,
    val next: NormalRawSegment?
) :
    RawSegment() {
    override fun toString(): String {
        return "${text ?: ""}${parts?.toString() ?: ""}${next?.toString() ?: ""}"
    }
}

data class WildCardRawSegment(val prefix: Text?, val name: Text?, val suffix: Text?) :
    RawSegment() {
    override fun toString(): String {
        return "${prefix ?: ""}{${name ?: ""}}${suffix ?: ""}"
    }
}

object PrefixRawSegment : RawSegment() {
    override fun toString(): String {
        return "**"
    }
}

typealias RawSegments = List<RawSegment>

internal class TokenMismatchException : Exception {
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor(msg: String) : super(msg)
}

internal class InvalidUriException :
    RuntimeException {
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor(msg: String) : super(msg)
}
