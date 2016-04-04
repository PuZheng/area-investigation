package com.puzheng.area_investigation.model

import android.content.ContentValues
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

data class POI(val id: Long?, val poiTypeUUID: String, val latLng: LatLng, val created: Date, val updated: Date? = null) {

    class Model {
        companion object {

            val TABLE_NAME = "poi"
            val COL_LAT_LNG = "lat_lng"
            val COL_POI_TYPE_UUID = "poi_type_uuid"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"


            val CREATE_SQL: String
                get() = """
                    CREATE TABLE ${Area.Model.TABLE_NAME} (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        ${POI.Model.COL_POI_TYPE_UUID} TEXT NOT NULL,
                        ${POI.Model.COL_LAT_LNG} TEXT NOT NULL,
                        ${POI.Model.COL_CREATED} TEXT NOT NULL,
                        ${POI.Model.COL_UPDATED} TEXT
                    )
                """

            fun makeValues(poi: POI) = ContentValues().apply {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                poi.latLng.let {
                    put(POI.Model.COL_LAT_LNG, "${it.latitude},${it.longitude}")
                }
                put(POI.Model.COL_POI_TYPE_UUID, poi.poiTypeUUID)
                put(POI.Model.COL_CREATED, format.format(poi.created))
                put(POI.Model.COL_UPDATED, if (poi.updated != null) format.format(poi.updated) else null)
            }

        }
    }

}