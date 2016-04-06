package com.puzheng.area_investigation.store

import android.content.Context
import android.os.Environment
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.copyTo
import com.puzheng.area_investigation.model.POIType
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

class POITypeStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POITypeStore(context)

    }

    val dir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(context.packageName), "poi_types")
    }

    private fun getPOITypeDir(poiType: POIType) = File(dir, poiType.path)

    fun getPOITypeIcon(poiType: POIType) = File(getPOITypeDir(poiType), "ic.png")

    fun getPOITypeActiveIcon(poiType: POIType) = File(getPOITypeDir(poiType), "ic_active.png")

    fun fakePoiTypes() = task {
        listOf("bus" to "公交站", "exit" to "出入口", "emergency" to "急救站").forEach {
            File(dir, it.first).apply {
                mkdirs()
                File(this, "config.json").apply {
                    if (!exists()) {
                        createNewFile()
                    }
                    writeText(JSONObject().apply {
                        put("uuid", UUID.randomUUID().toString())
                        put("name", it.second)
                    }.toString())
                }
                context.assets.open("icons/ic_${it.first}.png").copyTo(File(this, "ic.png"))
                context.assets.open("icons/ic_${it.first}_active.png").copyTo(File(this, "ic_active.png"))
            }
        }
    }

    val list: Promise<List<POIType>?, Exception>
        get() = task {
            dir.listFiles({ file -> file.isDirectory })?.map {
                Logger.v("${it.path}, ${it.name}")
                val configFile = it.listFiles { file, fname -> fname == "config.json" }.getOrNull(0)
                if (configFile?.exists() ?: false) {
                    try {
                        val json = JSONObject(configFile!!.readText())
                        POIType(json.getString("uuid"), json.getString("name"), it.name)
                    } catch (e: JSONException) {
                        Logger.e(e.toString())
                        null
                    }
                } else {
                    null
                }
            }?.filter {
                it != null
            }?.map {
                it!!
            }
            // TODO if met a unzipped zip file (no corresponding dir or phased out), zip it
        }
}
