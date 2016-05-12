package com.puzheng.region_investigation.model

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.DBHelper
import com.puzheng.region_investigation.MyApplication
import com.puzheng.region_investigation.getPOIRow
import nl.komponents.kovenant.task
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


data class Region(val id: Long?, var name: String, var outline: List<LatLng>, val created: Date,
                  var updated: Date? = null, var synced: Date? = null) : Parcelable {

    class Model : BaseColumns {

        companion object {
            val TABLE_NAME = "region"
            val COL_NAME = "name"
            val COL_OUTLINE = "outline"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"
            val COL_SYNCED = "synced"

            val CREATE_SQL: String
                get() = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT NOT NULL UNIQUE,
                        $COL_OUTLINE TEXT NOT NULL,
                        $COL_CREATED TEXT NOT NULL,
                        $COL_UPDATED TEXT,
                        $COL_SYNCED TEXT
                    )
                """

            fun makeValues(region: Region) = ContentValues().apply {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                put(COL_NAME, region.name)
                put(COL_OUTLINE, encodeOutline(region.outline))
                put(COL_CREATED, format.format(region.created))
                put(COL_UPDATED, if (region.updated != null) format.format(region.updated) else null)
                put(COL_SYNCED, if (region.synced != null) format.format(region.synced) else null)
            }
        }
    }

    val isDirty: Boolean
        get() = (synced == null || synced!! < updated)


    constructor(source: Parcel) : this(
            source.readSerializable() as Long?,
            source.readString(),
            decodeOutline(source.readString()),
            source.readSerializable() as Date,
            source.readSerializable() as Date?,
            source.readSerializable() as Date?)

    /**
     * 区域面积， 参考http://www.wikihow.com/Calculate-the-Area-of-a-Polygon
     */
    val area: Double
        get() {
            if (outline.isEmpty()) {
                return 0.0
            }
            // 以出发点作为(0, 0), 注意，由于火星坐标系的原因，这里不能直接采用mercator投影
            val start = outline[0]
            val t = listOf(Pair(0.0, 0.0)) + outline.subList(1, outline.size).map {
                Pair(
                        (if (it.latitude > start.latitude) 1 else -1) *
                                AMapUtils.calculateLineDistance(start, LatLng(it.latitude, start.longitude)).toDouble(),
                        (if (it.longitude > start.longitude) 1 else -1) *
                                AMapUtils.calculateLineDistance(start, LatLng(start.latitude, it.longitude)).toDouble()
                )
            } + listOf(Pair(0.0, 0.0))
            Logger.v(t.toString())
            return Math.abs(((0..t.size - 2).map {
                t[it].first * t[it + 1].second
            }.sum() - (0..t.size - 2).map {
                t[it].second * t[it + 1].first
            }.sum())) / 2
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(id)
        dest?.writeString(name)
        dest?.writeString(encodeOutline(outline))
        dest?.writeSerializable(created)
        dest?.writeSerializable(updated)
        dest?.writeSerializable(synced)
    }


    companion object {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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

    fun jsonizeSync(jsonObject: JSONObject) {
        jsonObject.apply {
            put("id", id)
            put("name", name)
            put("outline", encodeOutline(outline))
            put("created", format.format(created))
            put("updated", format.format(updated))
            put("pois", JSONArray().apply {
                poiListSync?.forEach {
                    put(JSONObject().apply {
                        it.jsonize(this)
                    })
                }
            })
        }
    }

    private val dbHelper: DBHelper by lazy {
        DBHelper(MyApplication.context)
    }

    val poiListSync: List<POI>?
        get() = dbHelper.withDb {
            db ->
            try {
                val cursor = db.query(POI.Model.TABLE_NAME, null, "${POI.Model.COL_REGION_ID}=?", arrayOf(id.toString()),
                        null, null, null)
                var rows: List<POI>? = null
                if (cursor.moveToFirst()) {
                    rows = mutableListOf()
                    do {
                        rows.add(cursor.getPOIRow())
                    } while (cursor.moveToNext())
                }
                cursor.close()
                rows
            } catch (e: Exception) {
                Logger.e(e.toString())
                null
            } finally {
                db.close()
            }
        }


    fun loadPOIList() = task {
        poiListSync
    }

    fun setSyncedSync() {
        dbHelper.withWritableDb {
            db ->
            try {
                db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                    put(Region.Model.COL_SYNCED, format.format(Date()))
                }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
            } finally {
                db.close()
            }
        }
    }

}