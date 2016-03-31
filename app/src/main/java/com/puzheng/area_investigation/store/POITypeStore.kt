package com.puzheng.area_investigation.store

import android.content.Context
import android.os.Environment
import com.puzheng.area_investigation.BuildConfig
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.openReadableFile
import org.json.JSONException
import org.json.JSONObject
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File

class POITypeStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POITypeStore(context)
    }

    private val dir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(null), "/poi_types").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun fakeData() = Observable.create<Void> {
        listOf("bus" to "公交站", "exit" to "出入口", "emergency_station" to "急救站").forEach {
            File(dir, it.first).apply {
                mkdirs()
                File(this, "config.json").writeText(JSONObject().apply {
                    put("name", it.second)
                }.toString())
            }
        }
        it!!.onNext(null)
    }.subscribeOn(Schedulers.computation())

    val list: Observable<List<POIType>>
        get() = Observable.create<List<POIType>> {
            val poiTypes = dir.listFiles({ file -> file.isDirectory }).map {
                val configFile = it.listFiles { file, fname -> fname == "config.json" }.getOrNull(0)
                if (configFile?.exists() ?: false) {
                    val json = JSONObject(configFile!!.readText())
                    try {
                        POIType(json.getString("name"))
                    } catch (e: JSONException) {
                        null
                    }
                } else {
                    null
                }
            }.filter {
                it != null
            }.map {
                it!!
            }
            it!!.onNext(poiTypes)
            // TODO if met a unzipped zip file (no corresponding dir or phased out), zip it
        }.subscribeOn(Schedulers.computation())
}
