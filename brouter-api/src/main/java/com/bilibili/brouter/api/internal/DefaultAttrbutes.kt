package com.bilibili.brouter.api.internal

import android.os.Parcel
import android.os.Parcelable
import com.bilibili.brouter.api.AttributeContainer
import com.bilibili.brouter.api.MutableAttributeContainer

/**
 * Not thread safe.
 */

interface AttributeContainerInternal : AttributeContainer, Parcelable {

    val attributesMap: Map<String, String>

    val asMutable: MutableAttributeContainerInternal
}

interface MutableAttributeContainerInternal : MutableAttributeContainer {
    fun asImmutable(copy: Boolean): AttributeContainerInternal
}

internal abstract class AbstractAttributeContainer :
    AttributeContainer {

    abstract val attributesMap: Map<String, String>

    override val isEmpty: Boolean
        get() = attributesMap.isEmpty()

    override fun getAttribute(key: String): String? = attributesMap[key]

    override val keySet: Set<String>
        get() = attributesMap.keys

    override fun contains(key: String): Boolean = attributesMap.containsKey(key)

    override val attributes: AttributeContainer
        get() = this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractAttributeContainer) return false

        if (attributesMap != other.attributesMap) return false

        return true
    }

    override fun hashCode(): Int {
        return attributesMap.hashCode()
    }

    override fun toString(): String {
        return "Attributes$attributesMap"
    }
}

internal class DefaultAttributeContainer(override val attributesMap: Map<String, String>) :
    AbstractAttributeContainer(),
    AttributeContainerInternal {
    override val asMutable: MutableAttributeContainerInternal
        get() = DefaultMutableAttributeContainer(attributesMap)

    constructor(parcel: Parcel) : this(linkedMapOf<String, String>()
        .apply {
            parcel.readMap(this, null)
        }
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeMap(attributesMap)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DefaultAttributeContainer> {
        override fun createFromParcel(parcel: Parcel): DefaultAttributeContainer {
            return DefaultAttributeContainer(parcel)
        }

        override fun newArray(size: Int): Array<DefaultAttributeContainer?> {
            return arrayOfNulls(size)
        }

        val EMPTY = DefaultAttributeContainer(emptyMap())
    }
}

internal class DefaultMutableAttributeContainer(
    private var refMap: Map<String, String> = emptyMap()
) :
    AbstractAttributeContainer(),
    MutableAttributeContainerInternal {

    private var actualMap: MutableMap<String, String>? = null

    override val attributesMap: Map<String, String>
        get() = actualMap ?: refMap

    private val mutableMap: MutableMap<String, String>
        get() = actualMap?.let {
            it
        } ?: refMap.let {
            refMap = emptyMap()
            LinkedHashMap(it).apply {
                actualMap = this
            }
        }

    override fun asImmutable(copy: Boolean): AttributeContainerInternal =
        (actualMap?.let {
            if (copy) {
                it.toMap()
            } else {
                it
            }
        } ?: refMap).let {
            if (it.isEmpty()) {
                DefaultAttributeContainer.EMPTY
            } else {
                DefaultAttributeContainer(it)
            }
        }

    override val keySet: MutableSet<String>
        get() = mutableMap.keys

    override fun attributes(action: (MutableAttributeContainer) -> Unit): MutableAttributeContainer =
        this.apply(action)

    override fun attribute(key: String, value: String): MutableAttributeContainer {
        mutableMap[key] = value
        return this
    }

    override val attributes: MutableAttributeContainer
        get() = this

}