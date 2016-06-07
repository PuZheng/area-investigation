package com.puzheng.region_investigation

import android.content.Context
import com.orhanobut.logger.Logger

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ConfigStore private constructor(context: Context) {

    companion object {
        fun with(context: Context): ConfigStore {
            return ConfigStore(context)
        }
    }

    private val config: Config? by lazy {
        try {
            val sb = StringBuilder()
            val reader = BufferedReader(
                    InputStreamReader(context.resources.assets.open("config.json")))
            while (true) {
                val line = reader.readLine() ?: break
                sb.append(line)
            }
            val jsonObject = JSONObject(sb.toString())
            Config(jsonObject.getString("backend"), jsonObject.getBoolean("fakeData"))
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

    private class Config(val backend: String, val fakeData: Boolean)
}