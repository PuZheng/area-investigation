package com.puzheng.area_investigation.model

import android.os.Parcel
import android.os.Parcelable

import android.content.ContentValues
import android.provider.BaseColumns
import java.text.SimpleDateFormat
import java.util.*


data class Area(val id: Long?, var name: String, val created: Date, var updated: Date? = null) : Parcelable {
    class Model : BaseColumns {

        companion object {
            val TABLE_NAME = "area"
            val COL_NAME = "name"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"

            val CREATE_SQL: String
                get() = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT NOT NULL,
                        $COL_CREATED TEXT NOT NULL,
                        $COL_UPDATED TEXT
                    )
                """

            fun makeValues(area: Area): ContentValues {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val contentValues = ContentValues()
                contentValues.put(COL_NAME, area.name)
                contentValues.put(COL_CREATED, format.format(area.created))
                contentValues.put(COL_UPDATED, if (area.updated != null) format.format(area.updated) else null)
                return contentValues
            }
        }

    }

    constructor(source: Parcel): this(source.readSerializable() as Long?, source.readString(), source.readSerializable() as Date, source.readSerializable() as Date?)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(id)
        dest?.writeString(name)
        dest?.writeSerializable(created)
        dest?.writeSerializable(updated)
    }

    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<Area> = object : Parcelable.Creator<Area> {
            override fun createFromParcel(source: Parcel): Area {
                return Area(source)
            }

            override fun newArray(size: Int): Array<Area?> {
                return arrayOfNulls(size)
            }
        }
    }
}

