package com.puzheng.area_investigation.store

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import com.puzheng.area_investigation.DBHelpler
import com.puzheng.area_investigation.model.POI
import nl.komponents.kovenant.task

class POIStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POIStore(context)
    }

    fun create(poi: POI) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(poi))
        } finally {
            db.close()
        }
    }

    fun remove(poi: POI) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.delete(POI.Model.TABLE_NAME, "${BaseColumns._ID}=?", arrayOf(poi.id.toString()))
        } finally {
            db.close()
        }
    }

    fun update(poi: POI, value: Map<String, Any?>) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.update(POI.Model.TABLE_NAME, ContentValues().apply {
                value.forEach {
                    val (k, v) = it
                    // TODO 这里只处理了经纬度
                    when (k) {
                        POI.Model.COL_LAT_LNG ->
                            (v as LatLng).let {
                                put(k, "${it.latitude},${it.longitude}")
                            }
                        else -> {}
                    }
                }
            }, "${BaseColumns._ID}=?", arrayOf(poi.id.toString()))
        } finally {
            db.close()
        }
    }
}