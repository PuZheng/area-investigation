package com.puzheng.region_investigation.store

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
            Config(jsonObject.getString("uploadBackend"))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    val uploadBackend: String?
        get() = config?.uploadBackend

    private class Config(val uploadBackend: String)
}