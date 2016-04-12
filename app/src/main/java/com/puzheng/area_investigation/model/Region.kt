package com.puzheng.area_investigation.model

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*


data class Region(val id: Long?, var name: String, var outline: List<LatLng>, val created: Date, var updated: Date? = null) : Parcelable {
    class Model : BaseColumns {

        companion object {
            val TABLE_NAME = "area"
            val COL_NAME = "name"
            val COL_OUTLINE = "outline"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"

            val CREATE_SQL: String
                get() = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT NOT NULL,
                        $COL_OUTLINE TEXT NOT NULL,
                        $COL_CREATED TEXT NOT NULL,
                        $COL_UPDATED TEXT
                    )
                """

            fun makeValues(region: Region) = ContentValues().apply {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                put(COL_NAME, region.name)
                put(COL_OUTLINE, encodeOutline(region.outline))
                put(COL_CREATED, format.format(region.created))
                put(COL_UPDATED, if (region.updated != null) format.format(region.updated) else null)
            }
        }

    }

    constructor(source: Parcel) : this(
            source.readSerializable() as Long?,
            source.readString(),
            decodeOutline(source.readString()),
            source.readSerializable() as Date,
            source.readSerializable() as Date?)

    /**
     * 计算区域面积， 参考http://www.mathopenref.com/heronsformula.html
     */
    val area: Double
        get() = (1..outline.size - 2).map {
            val a = AMapUtils.calculateLineDistance(outline[it], outline[0])
            val b = AMapUtils.calculateLineDistance(outline[it + 1], outline[0])
            val c = AMapUtils.calculateLineDistance(outline[it], outline[it + 1])
            val p = (a + b + c) / 2
            Math.sqrt((p * (p - a) * (p - b) * (p - c)).toDouble())
        }.sum()

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(id)
        dest?.writeString(name)
        dest?.writeString(encodeOutline(outline))
        dest?.writeSerializable(created)
        dest?.writeSerializable(updated)
    }


    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<Region> = object : Parcelable.Creator<Region> {
            override fun createFromParcel(source: Parcel): Region {
                return Region(source)
            }

            override fun newArray(size: Int): Array<Region?> {
                return arrayOfNulls(size)
            }
        }


        fun decodeOutline(s: String) = s.split(":").map {
            val (lat, lng) = it.split(",").map { it.toDouble() }
            LatLng(lat, lng)
        }

        fun encodeOutline(outline: List<LatLng>) =
                outline.map { "${it.latitude},${it.longitude}" }.joinToString(":")
    }
}