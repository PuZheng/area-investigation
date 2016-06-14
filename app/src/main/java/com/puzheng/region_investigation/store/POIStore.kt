package com.puzheng.region_investigation.store

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import com.puzheng.region_investigation.DBHelper
import com.puzheng.region_investigation.MyApplication
import com.puzheng.region_investigation.model.POI
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.json.JSONObject
import java.io.File
import java.util.logging.Level

class POIStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POIStore(context)
    }

    val dir: File by lazy {

        File(Environment.getExternalStoragePublicDirectory(context.packageName), "pois").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun create(poi: POI) = task {
        val db = DBHelper(context).writableDatabase
        db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(poi))
    } then {
        MyApplication.eventLogger.log(Level.INFO, "创建信息点", JSONObject().apply {
            put("type", EventType.CREATE_POI)
            put("id", it)
            put("regionId", poi.regionId)
        })
        it
    }

    fun remove(poi: POI) = task {
        val db = DBHelper(context).writableDatabase
        db.delete(POI.Model.TABLE_NAME, "${BaseColumns._ID}=?", arrayOf(poi.id.toString()))

        MyApplication.eventLogger.log(Level.INFO, "删除信息点", JSONObject().apply {
            put("type", EventType.DELETE_POI)
            put("id", poi.id)
            put("regionId", poi.regionId)
        })
    }

    fun update(poi: POI, value: Map<String, Any?>) = task {
        val db = DBHelper(context).writableDatabase
        db.update(POI.Model.TABLE_NAME, ContentValues().apply {
            value.forEach {
                val (k, v) = it
                // TODO 这里只处理了经纬度
                when (k) {
                    POI.Model.COL_LAT_LNG ->
                        (v as LatLng).let {
                            put(k, "${it.latitude},${it.longitude}")
                        }
                    else -> {
                    }
                }
            }
        }, "${BaseColumns._ID}=?", arrayOf(poi.id.toString()))
        MyApplication.eventLogger.log(Level.INFO, "修改信息点", JSONObject().apply {
            put("type", EventType.UPDATE_POI)
            put("id", poi.id)
            put("fields", JSONObject().apply {
                value.forEach {
                    val (k, v) = it
                    when (k) {
                        POI.Model.COL_LAT_LNG ->
                            put("lnglat", JSONObject().apply {
                                put("old", "${poi.latLng.latitude},${poi.latLng.longitude}")
                                put("new", "${(v as LatLng).latitude},${v.longitude}")
                            })
                        else -> {}
                    }
                }
            })
        })
    }
}