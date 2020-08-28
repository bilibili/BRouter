package com.bilibili.brouter.api.internal

import android.os.Parcel
import android.os.Parcelable
import com.bilibili.brouter.api.MultiMap
import com.bilibili.brouter.api.MutableMultiMap
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * Shallow copy for list, not thread safe.
 * @author dieyi
 * Created at 2020/3/12.
 */

interface MultiMapInternal : MultiMap {
    val asMutable: MutableMultiMapInternal
}

interface MutableMultiMapInternal : MutableMultiMap {
    fun asImmutable(copy: Boolean): MultiMapInternal
}

internal abstract class AbstractMultiMap : MultiMap {
    protected abstract val map: Map<String, MutableList<String>>

    override val keySet: Set<String>
        get() = map.keys

    override operator fun get(key: String): String? = map[key]?.first()

    override val size: Int
        get() = map.size
    override val isEmpty: Boolean
        get() = map.isEmpty()

    override fun getLast(key: String): String? = map[key]?.last()

    override fun getAll(key: String): List<String>? = map[key]?.let {
        Collections.unmodifiableList(it)
    }

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeMap(map)
    }


    override fun describeContents(): Int = 0
    override fun toString(): String {
        return map.toString()
    }
}

internal class DefaultMutableMultiMap(
    private var refMap: Map<String, MutableList<String>> = emptyMap()
) :
    AbstractMultiMap(), MutableMultiMapInternal {

    private var actualMap: MutableMap<String, MutableList<String>>? = null

    override val map: Map<String, MutableList<String>>
        get() = actualMap ?: refMap

    private val mutableMap: MutableMap<String, MutableList<String>>
        get() = actualMap?.let {
            it
        } ?: refMap.let {
            refMap = emptyMap()
            LinkedHashMap(it).apply {
                actualMap = this
            }
        }

    override val keySet: MutableSet<String>
        get() = mutableMap.keys

    override fun clear() {
        mutableMap.clear()
    }

    constructor(parcel: Parcel) : this() {
        actualMap = parcel.readHashMap(null) as MutableMap<String, MutableList<String>>
    }

    private fun listForKey(key: String): MutableList<String> = mutableMap.getOrPut(key) {
        // one value for most case
        ArrayList(1)
    }

    override fun append(key: String, value: String): MutableMultiMap {
        listForKey(key) += value
        return this
    }


    override fun appendAll(key: String, values: Collection<String>): MutableMultiMap {
        if (values.isNotEmpty()) {
            listForKey(key) += values
        }
        return this
    }

    override fun put(key: String, value: String): MutableList<String>? {
        return mutableMap.put(key, ArrayList<String>(1).apply {
            this += value
        })
    }

    override fun putAll(key: String, values: Collection<String>): MutableList<String>? {
        return if (!values.isEmpty()) {
            mutableMap.put(key, values.toMutableList())
        } else {
            remove(key)
        }
    }

    override fun remove(key: String): MutableList<String>? {
        return mutableMap.remove(key)
    }

    override fun asImmutable(copy: Boolean): MultiMapInternal = (actualMap?.let {
        if (copy) {
            it.toMap()
        } else {
            it
        }
    } ?: refMap).let {
        if (it.isEmpty()) {
            DefaultMultiMap.EMPTY
        } else {
            DefaultMultiMap(it)
        }
    }

    companion object CREATOR : Parcelable.Creator<DefaultMutableMultiMap> {
        override fun createFromParcel(parcel: Parcel): DefaultMutableMultiMap {
            return DefaultMutableMultiMap(parcel)
        }

        override fun newArray(size: Int): Array<DefaultMutableMultiMap?> {
            return arrayOfNulls(size)
        }
    }
}

internal class DefaultMultiMap(override val map: Map<String, MutableList<String>> = emptyMap()) :
    AbstractMultiMap(), MultiMapInternal {
    override val asMutable: MutableMultiMapInternal
        get() = DefaultMutableMultiMap(map)

    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) : this(parcel.readHashMap(null) as Map<String, MutableList<String>>)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeMap(map)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DefaultMultiMap> {
        override fun createFromParcel(parcel: Parcel): DefaultMultiMap {
            return DefaultMultiMap(parcel)
        }

        override fun newArray(size: Int): Array<DefaultMultiMap?> {
            return arrayOfNulls(size)
        }

        val EMPTY = DefaultMultiMap()
    }
}