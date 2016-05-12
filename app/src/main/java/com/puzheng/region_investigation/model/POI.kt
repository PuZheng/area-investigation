package com.puzheng.region_investigation.model

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.MyApplication
import com.puzheng.region_investigation.store.EventType
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.RegionStore
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

data class POI(val id: Long?, val poiTypeUUID: String, val regionId: Long, val latLng: LatLng, val created: Date,
               val updated: Date?=null) : Parcelable {

    val dir: File by lazy {
        File(POIStore.dir, "$id").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    val dataFile: File by lazy {
        File(dir, "data.json")
    }

    fun saveData(json: JSONObject) = task {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        if (!dataFile.exists()) {
            dataFile.createNewFile()
        }
        dataFile.writeText(json.toString())
        MyApplication.eventLogger.log(Level.INFO, "修改信息点附属信息", JSONObject().apply {
            put("type", EventType.UPDATE_POI)
            put("id", id)
            put("data", json.toString())
        })

    } and RegionStore.with(MyApplication.context).touch(regionId)

    class Model {
        companion object {

            val TABLE_NAME = "poi"
            val COL_LAT_LNG = "lat_lng"
            val COL_REGION_ID = "region_id"
            val COL_POI_TYPE_UUID = "poi_type_uuid"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"


            val CREATE_SQL: String
                get() = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_POI_TYPE_UUID TEXT NOT NULL,
                        $COL_LAT_LNG TEXT NOT NULL,
                        $COL_CREATED TEXT NOT NULL,
                        $COL_UPDATED TEXT,
                        $COL_REGION_ID INTEGER,
                        FOREIGN KEY($COL_REGION_ID) REFERENCES ${Region.Model.TABLE_NAME}(${BaseColumns._ID})
                    )
                """

            fun makeValues(poi: POI) = ContentValues().apply {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                poi.latLng.let {
                    put(POI.Model.COL_LAT_LNG, "${it.latitude},${it.longitude}")
                }
                put(COL_REGION_ID, poi.regionId)
                put(COL_POI_TYPE_UUID, poi.poiTypeUUID)
                put(COL_CREATED, format.format(poi.created))
                put(COL_UPDATED, if (poi.updated != null) format.format(poi.updated) else null)
            }

        }
    }

    constructor(source: Parcel): this(source.readSerializable() as Long?, source.readString(), source.readLong(), source.readParcelable<LatLng>(LatLng::class.java.classLoader), source.readSerializable() as Date, source.readSerializable() as Date?)

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(id)
        dest?.writeString(poiTypeUUID)
        dest?.writeLong(regionId)
        dest?.writeParcelable(latLng, 0)
        dest?.writeSerializable(created)
        dest?.writeSerializable(updated)
    }

    companion object {
        @JvmField final val CREATOR: Parcelable.Creator<POI> = object : Parcelable.Creator<POI> {
            override fun createFromParcel(source: Parcel): POI {
                return POI(source)
            }

            override fun newArray(size: Int): Array<POI?> {
                return arrayOfNulls(size)
            }
        }

        fun decodeLatLng(s: String): LatLng {
            val (lat, lng) = s.split(",").map { it.toDouble() }
            return LatLng(lat, lng)
        }
    }

    fun jsonize(jsonObject: JSONObject) {
        jsonObject.apply {
            put("id", id)
            put("poiType", JSONObject().apply {
                put("uuid", poiTypeUUID)
            })
            put("regionId", regionId)
            put("latLng", JSONObject().apply {
                put("lat", latLng.latitude)
                put("lng", latLng.longitude)
            })
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            put("created", format.format(created))
            if (updated != null) {
                put("updated", format.format(updated))
            }
        }
    }
}