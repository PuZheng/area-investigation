package com.puzheng.region_investigation.model

import java.util.*
import android.os.Parcel
import android.os.Parcelable

import nl.komponents.kovenant.task
import org.json.JSONException
import org.json.JSONObject

data class POIType(val name: String, val timestamp: String, val fields: List<Field>,
                   val path: String) : Parcelable {
    fun extractPOIRawData(poi: POI) = task {
        if (poi.dataFile.exists()) {
            val json = JSONObject(poi.dataFile.readText())
            mapOf(*fields.map {
                it.name to try {
                    json.get(it.name)
                } catch (e: JSONException) {
                    null
                }
            }.toTypedArray())
        } else {
            null
        }
    }

    enum class FieldType {
        STRING, TEXT, VIDEO, IMAGES
    }

    data class Field(val name: String, val type: FieldType, val attrs: Map<String, String>?)

    constructor(source: Parcel): this(source.readString(),
            source.readString(),
            ArrayList<Field>().apply{ source.readList(this, Field::class.java.classLoader) },
            source.readString())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeString(timestamp)
        dest?.writeList(fields)
        dest?.writeString(path)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<POIType> = object : Parcelable.Creator<POIType> {
            override fun createFromParcel(source: Parcel): POIType = POIType(source)
            override fun newArray(size: Int): Array<POIType?> = arrayOfNulls(size)
        }
    }
}
