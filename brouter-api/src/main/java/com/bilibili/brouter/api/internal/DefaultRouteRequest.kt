package com.bilibili.brouter.api.internal

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.bilibili.brouter.api.*

internal class DefaultRouteRequest private constructor(uri: Uri?, builder: Builder?) :
    RouteRequest {
    private var parsed: Boolean

    override val pureUri: Uri by lazy {
        if (targetUri.isOpaque) {
            targetUri
        } else {
            targetUri.buildUpon()
                .apply {
                    val sb = StringBuilder().apply {
                        _params.let {
                            if (!it.isEmpty) {
                                appendParams(it)
                            }
                        }
                    }
                    if (sb.isNotEmpty()) {
                        encodedQuery(sb.toString())
                    }
                }
                .build()
        }
    }

    override val uniformUri: Uri by if (uri != null) lazyOf(uri) else lazy {
        if (targetUri.isOpaque) {
            targetUri
        } else {
            targetUri.buildUpon()
                .apply {
                    val sb = StringBuilder().apply {
                        // append attrs first
                        _attributes.let {
                            if (!it.isEmpty) {
                                appendAttrs(it)
                            }
                        }

                        _flags.let {
                            if (it != 0) {
                                appendFlags(it)
                            }
                        }

                        _requestCode.let {
                            if (it >= 0) {
                                appendRequestCode(it)
                            }
                        }

                        data?.let {
                            appendData(it)
                        }

                        _routeTypes.let {
                            if (it.isNotEmpty()) {
                                appendRouteTypes(it)
                            }
                        }

                        _prev.let {
                            if (it != null) {
                                appendPrev(it)
                            }
                        }

                        _forward.let {
                            if (it != null) {
                                appendForward(it)
                            }
                        }

                        _props.let {
                            if (!it.isEmpty) {
                                appendProps(it)
                            }
                        }

                        _params.let {
                            if (!it.isEmpty) {
                                appendParams(it)
                            }
                        }
                    }
                    if (sb.isNotEmpty()) {
                        encodedQuery(sb.toString())
                    }
                }
                .build()
        }
    }

    private var _targetUri: Uri
    override val targetUri: Uri
        get() = checkParsed().let {
            _targetUri
        }

    private var _attributes: AttributeContainerInternal
    override val attributes: AttributeContainer get() = _attributes

    private fun checkParsed() {
        if (!parsed) {
            synchronized(this) {
                if (!parsed) {
                    val uri = uniformUri
                    //  target
                    if (uri.isHierarchical) {
                        _targetUri = uri.buildUpon().query(null).build()
                        uri.queryMap?.let {
                            // others, 按照顺序解析
                            _flags = it.parseFlags()
                            _requestCode = it.parseRequestCode()
                            _data = it.parseData()
                            _routeTypes = it.parseRouteTypes()
                            _prev = it.parsePrev()
                            _forward = it.parseForward()
                            _attributes = it.parseAttrs().asImmutable(false)
                            _props = it.parseProps().asImmutable(false)
                            _params = it.parseParams().asImmutable(false)
                        }
                    } else {
                        _targetUri = uri
                    }
                    parsed = true
                }
            }
        }
    }

    private var _flags: Int
    override val flags: Int get() = checkParsed().let { _flags }

    private var _requestCode: Int
    override val requestCode: Int get() = checkParsed().let { _requestCode }

    private var _data: Uri?
    override val data: Uri? get() = checkParsed().let { _data }

    private var _routeTypes: List<String>
    override val routeTypes: List<String> get() = checkParsed().let { _routeTypes }

    private var _prev: RouteRequest?
    override val prev: RouteRequest? get() = checkParsed().let { _prev }

    private var _forward: RouteRequest?
    override val forward: RouteRequest? get() = checkParsed().let { _forward }

    private var _props: MultiMapInternal
    override val props: MultiMap get() = checkParsed().let { _props }

    private var _params: MultiMapInternal
    override val params: MultiMap get() = checkParsed().let { _params }


    override val options: Bundle?
    override val extras: Bundle?
    override val animIn: Int
    override val animOut: Int


    override fun writeToParcel(dest: Parcel, flags: Int) {
        checkParsed()
        dest.writeParcelable(_targetUri, 0)
        dest.writeParcelable(_attributes, 0)
        dest.writeInt(_flags)
        dest.writeInt(_requestCode)
        dest.writeParcelable(_data, 0)
        dest.writeList(_routeTypes)
        dest.writeParcelable(_prev, 0)
        dest.writeParcelable(_forward, 0)
        dest.writeParcelable(_props, 0)
        dest.writeParcelable(_params, 0)

        dest.writeBundle(extras)
        dest.writeBundle(options)
        dest.writeInt(animIn)
        dest.writeInt(animOut)
    }

    override fun describeContents(): Int = 0

    init {
        if (builder != null) {
            parsed = true
            _targetUri = builder.targetUri
            _attributes = builder._attributes.asImmutable(true)
            _flags = builder.flags
            _requestCode = builder.requestCode
            _data = builder.data
            _routeTypes = builder.routeTypes
            _prev = builder.prev
            _forward = builder.forward
            _props = builder._props.asImmutable(true)
            _params = builder._params.asImmutable(true)

            extras = builder.extras?.let {
                Bundle(it)
            }
            options = builder.options
            animIn = builder.animIn
            animOut = builder.animOut
        } else {
            assert(uri != null)
            parsed = false
            _targetUri = Uri.EMPTY
            _attributes = DefaultAttributeContainer.EMPTY
            _flags = 0
            _requestCode = -1
            _data = null
            _routeTypes = emptyList()
            _prev = null
            _forward = null
            _props = DefaultMultiMap.EMPTY
            _params = DefaultMultiMap.EMPTY

            extras = null
            options = null
            animIn = -1
            animOut = -1
        }
    }

    internal constructor(builder: Builder) : this(null, builder)

    internal constructor(uri: Uri) : this(uri, null)

    override fun newBuilder() = Builder(this)

    override fun toPrettyString(): String {
        return StringBuilder(128)
            .apply {
                appendToWithPrefix(this, "RouteRequest", 0, true)
            }
            .toString()
    }

    override fun toString(): String {
        return "RouteRequest(targetUri=$targetUri, flags=$flags, requestCode=$requestCode, data=$data, routeTypes=$routeTypes, prev=$prev, forward=$forward, extras=$extras, options=$options, animIn=$animIn, animOut=$animOut)"
    }

    internal class Builder : RouteRequest.Builder {
        internal var targetUri: Uri
        internal val _attributes: MutableAttributeContainerInternal
        override val attributes: MutableAttributeContainer get() = _attributes
        internal var flags: Int
        internal var requestCode: Int
        internal var data: Uri?
        internal var routeTypes: List<String>
        internal var prev: RouteRequest?
        internal var forward: RouteRequest?
        internal val _props: MutableMultiMapInternal
        override val props: MutableMultiMap get() = _props
        internal val _params: MutableMultiMapInternal
        override val params: MutableMultiMap get() = _props

        internal var extras: Bundle?
        internal var options: Bundle?
        internal var animIn: Int
        internal var animOut: Int

        internal constructor(request: DefaultRouteRequest) {
            request.checkParsed()

            targetUri = request._targetUri
            _attributes = request._attributes.asMutable
            flags = request._flags
            requestCode = request._requestCode
            data = request._data
            routeTypes = request._routeTypes
            prev = request._prev
            forward = request._forward
            _props = request._props.asMutable
            _params = request._params.asMutable

            extras = request.extras?.let {
                Bundle(it)
            }
            options = request.options
            animIn = request.animIn
            animOut = request.animOut
        }

        constructor(uri: String) : this(Uri.parse(uri))

        constructor(uri: Uri) {
            this.targetUri = if (uri.isHierarchical) {
                uri
                    .buildUpon()
                    .query(null)
                    .build()
            } else {
                uri
            }

            val q = uri.queryMap

            if (q != null) {
                // others, 按照顺序解析
                _attributes = q.parseAttrs()
                flags = q.parseFlags()
                requestCode = q.parseRequestCode()
                data = q.parseData()
                routeTypes = q.parseRouteTypes()
                prev = q.parsePrev()
                forward = q.parseForward()
                _props = q.parseProps()
                _params = q.parseParams()
            } else {
                _attributes = DefaultMutableAttributeContainer()
                flags = 0
                requestCode = -1
                data = null
                routeTypes = emptyList()
                prev = null
                forward = null
                _props = DefaultMutableMultiMap()
                _params = DefaultMutableMultiMap()
            }
            extras = null
            options = null
            animIn = -1
            animOut = -1
        }

        internal constructor(parcel: Parcel, cl: ClassLoader?) {
            this.targetUri = parcel.readParcelable(Uri::class.java.classLoader)
            this._attributes = parcel.readParcelable<AttributeContainerInternal>(cl).asMutable
            this.flags = parcel.readInt()
            this.requestCode = parcel.readInt()
            this.data = parcel.readParcelable(Uri::class.java.classLoader)
            this.routeTypes = parcel.readArrayList(cl) as List<String>
            this.prev = parcel.readParcelable(cl)
            this.forward = parcel.readParcelable(cl)
            this._props = parcel.readParcelable<MultiMapInternal>(cl).asMutable
            this._params = parcel.readParcelable<MultiMapInternal>(cl).asMutable

            this.extras = parcel.readBundle(cl)
            this.options = parcel.readBundle(cl)
            this.animIn = parcel.readInt()
            this.animOut = parcel.readInt()
        }

        override fun targetUri(uri: Uri) = this.apply {
            require(uri.query == null) {
                "Illegal uri $uri"
            }
            this.targetUri = uri
        }

        override fun attributes(action: (MutableAttributeContainer) -> Unit): Builder = this.apply {
            attributes.attributes(action)
        }

        override fun addFlag(flags: Int) = this.apply {
            this.flags = this.flags or flags
        }

        override fun flags(flags: Int) = this.apply {
            this.flags = flags
        }

        override fun requestCode(requestCode: Int) = this.apply {
            this.requestCode = requestCode
        }

        override fun data(data: Uri?) = this.apply {
            this.data = data
        }

        override fun routeTypes(types: List<String>): RouteRequest.Builder = this.apply {
            this.routeTypes = types
        }

        override fun routeTypes(vararg type: String): RouteRequest.Builder =
            routeTypes(type.asList())

        override fun prev(prev: RouteRequest?) = this.apply {
            this.prev = prev
        }

        override fun forward(forward: RouteRequest?) = this.apply {
            this.forward = forward
        }

        override fun props(configure: (MutableMultiMap) -> Unit) = this.apply {
            configure(_props)
        }

        override fun params(configure: (MutableMultiMap) -> Unit) = this.apply {
            configure(_params)
        }

        override fun extras(configure: (Bundle) -> Unit) = this.apply {
            configure(extras ?: Bundle().apply {
                extras = this
            })
        }

        override fun extras(extras: Bundle?) = this.apply {
            this.extras = extras
        }

        override fun options(configure: (Bundle) -> Unit) = this.apply {
            configure(options ?: Bundle().apply {
                options = this
            })
        }

        override fun options(options: Bundle?) = this.apply {
            this.options = options
        }

        override fun overridePendingTransition(animIn: Int, animOut: Int) = this.apply {
            this.animIn = animIn
            this.animOut = animOut
        }

        override fun build(): RouteRequest = DefaultRouteRequest(this)

        override fun toString(): String {
            return "RouteRequest.Builder(targetUri=$targetUri, flags=$flags, requestCode=$requestCode, data=$data, routeTypes=$routeTypes, prev=$prev, forward=$forward, extras=$extras, options=$options, animIn=$animIn, animOut=$animOut)"
        }
    }

    companion object CREATOR : Parcelable.ClassLoaderCreator<RouteRequest> {

        override fun createFromParcel(parcel: Parcel): RouteRequest =
            createFromParcel(parcel, RouteRequest::class.java.classLoader)

        override fun createFromParcel(parcel: Parcel, classLoader: ClassLoader?): RouteRequest {
            return Builder(parcel, classLoader).build()
        }

        override fun newArray(size: Int): Array<RouteRequest?> {
            return arrayOfNulls(size)
        }
    }
}


internal fun RouteRequest.appendToWithPrefix(
    builder: StringBuilder,
    name: String,
    i: Int,
    includePrev: Boolean
) {
    builder.appendPrefix(i)
        .append(name)
        .append(" TargetUri: ")
        .append(targetUri)
        .append('\n')

        .appendPrefix(i)
        .append(' ')
        .append(attributes)
        .append('\n')

        .appendPrefix(i)
        .append(" Flags: 0x")
        .append(java.lang.Integer.toHexString(flags))
        .append(" RequestCode: $requestCode")
        .append(" Data: ")
        .append(data)
        .append('\n')

        .appendPrefix(i)
        .append(" RouteTypes: ")
        .append(routeTypes)
        .append(" Anim ($animIn, $animOut)")
        .append(" Options: ")
        .append(options)
        .append('\n')


        .appendPrefix(i)
        .append(" Extras: ")
        .append(extras)
        .append('\n')

        .appendPrefix(i)
        .append(" Props: ")
        .append(props)
        .append('\n')

    forward?.appendToWithPrefix(builder, "ForwardRequest", i + 1, includePrev)
    if (includePrev) {
        prev?.appendToWithPrefix(builder, "PrevRequest", i + 1, true)
    }
}

internal fun StringBuilder.appendPrefix(i: Int): StringBuilder {
    for (j in 0 until i) {
        this.append('\t')
    }
    return this
}