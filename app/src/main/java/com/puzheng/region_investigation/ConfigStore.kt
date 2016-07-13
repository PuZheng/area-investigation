package com.puzheng.region_investigation

import android.content.Context
import android.os.Environment

import org.json.JSONException
import org.json.JSONObject
import java.io.*

class ConfigStore private constructor(context: Context) {

    companion object {
        fun with(context: Context): ConfigStore {
            return ConfigStore(context)
        }
    }

    val configFile = File(Environment.getExternalStoragePublicDirectory(context.packageName), "config.json")

    private val config: Config? by lazy {
        try {
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(FileInputStream(configFile)))
            while (true) {
                val line = reader.readLine() ?: break
                sb.append(line)
            }
            val jsonObject = JSONObject(sb.toString())
            Config(jsonObject.getString("backend"), jsonObject.getBoolean("fakeData"),
                    jsonObject.getString("offlineMapDir"),
                    jsonObject.getString("defaultUsername"),
                    jsonObject.getString("defaultOrgCode"),
                    jsonObject.getString("defaultOrgName"))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    val backend = config?.backend
    val fakeData = config?.fakeData ?: false
    val defaultUsername = config?.defaultUsername ?: ""
    val defaultOrgCode = config?.defaultOrgCode ?: ""
    val defaultOrgName = config?.defaultOrgName ?: ""

    val offlineMapDataDir = File(Environment.getExternalStoragePublicDirectory(""), config?.offlineMapDir ?: "autonavi/").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private class Config(val backend: String, val fakeData: Boolean, val offlineMapDir: String,
                         val defaultUsername: String, val defaultOrgCode: String,
                         val defaultOrgName: String)
}