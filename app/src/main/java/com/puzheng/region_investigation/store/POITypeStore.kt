package com.puzheng.region_investigation.store

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.ConfigStore
import com.puzheng.region_investigation.copyTo
import com.puzheng.region_investigation.model.POIType
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

class POITypeStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POITypeStore(context)

    }

    val dir: File by lazy {
        File(Environment.getExternalStoragePublicDirectory(context.packageName), "poi_types").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun getPOITypeDir(poiType: POIType) = File(dir, poiType.path)

    private fun makePOITypeUrl(orgCode: String, name: String) = Uri.parse(ConfigStore.with(context).backend).buildUpon()
                                    .appendEncodedPath("poi-type")
                                    .appendEncodedPath(orgCode).appendEncodedPath("$name.zip").build().toString()

    fun getPOITypeIcon(poiType: POIType) = File(getPOITypeDir(poiType), "ic.png")

    fun getPOITypeActiveIcon(poiType: POIType) = File(getPOITypeDir(poiType), "ic_active.png")

    fun fakePoiTypes() = task {
        listOf("bus" to "公交站", "exit" to "出入口", "emergency" to "急救站").forEach {
            File(dir, it.second).apply {
                mkdirs()
                File(this, "config.json").apply {
                    if (!exists()) {
                        createNewFile()
                    }
                    writeText(JSONObject().apply {
                        put("timestamp", "20160328091313")
                        put("name", it.second)
                        val jsonArray = JSONArray()
                        jsonArray.put(JSONObject().apply field@ {
                            this@field.put("name", "字段一")
                            this@field.put("type", "STRING")
                        })
                        jsonArray.put(JSONObject().apply field@ {
                            this@field.put("name", "字段二")
                            this@field.put("type", "TEXT")
                        })
                        jsonArray.put(JSONObject().apply field@ {
                            this@field.put("name", "字段三")
                            this@field.put("type", "IMAGES")
                        })
                        jsonArray.put(JSONObject().apply field@ {
                            this@field.put("name", "字段四")
                            this@field.put("type", "VIDEO")
                        })
                        put("fields", jsonArray)
                    }.toString())
                }
                context.assets.open("icons/ic_${it.first}.png").copyTo(File(this, "ic.png"))
                context.assets.open("icons/ic_${it.first}_active.png").copyTo(File(this, "ic_active.png"))
            }
        }
    }

    private val zipFileRegex = Pattern.compile(".*\\.zip$", Pattern.CASE_INSENSITIVE).toRegex()
    private val zipSuffixRegex = Pattern.compile("\\.zip$", Pattern.CASE_INSENSITIVE).toRegex()

    private fun tryUnzipSync() {
        dir.listFiles({ file -> file.name.matches(zipFileRegex) })?.map {
            unzip(it, File(it.path.replace(zipSuffixRegex, "")).apply{
                mkdirs()
            })
            it.delete()
        }
    }

    val listSync: List<POIType>?
        get() {
            tryUnzipSync()
            return dir.listFiles({ file -> file.isDirectory })?.map {
                val configFile = it.listFiles { file, fname -> fname == "config.json" }.getOrNull(0)
                if (configFile?.exists() ?: false) {
                    try {
                        val json = JSONObject(configFile!!.readText())
                        val jsonArray = json.getJSONArray("fields")
                        POIType(it.name, json.getString("timestamp"),
                                (0..jsonArray.length() - 1).map {
                                    val o = jsonArray.getJSONObject(it)
                                    POIType.Field(o.getString("name"),
                                            POIType.FieldType.valueOf(o.getString("type").toUpperCase()),
                                            null)
                                }, it.name)
                    } catch (e: JSONException) {
                        Logger.e(e.toString())
                        null
                    } catch (e: IllegalArgumentException) {
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
        }

    val list: Promise<List<POIType>?, Exception>
        get() = task {
            listSync
        }

    fun get(name: String) = list then {
        it?.find { it.name == name }
    }

    fun upgrade() = task {
        val poiTypeMap = mapOf(*if (listSync == null) {
            arrayOf<Pair<String, POIType>>()
        } else {
            listSync!!.map {
                it.name to it
            }.toTypedArray()
        })

        val orgCode = AccountStore.with(context).account!!.orgCode
        val response = OkHttpClient().newCall(
                Request.Builder()
                        .url(Uri.parse(ConfigStore.with(context).backend).buildUpon()
                                .appendEncodedPath("poi-type/latest-versions")
                                .appendQueryParameter("org_code", orgCode).build().toString())
                        .build()).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code " + response)
        }
        val root = JSONObject(response.body().string())
        val jsonArray = root.getJSONArray("data")
        val toBeUpgraded = (0..jsonArray.length()-1).map {
            val o = jsonArray.getJSONObject(it)
            val name = o.getString("name")
            val newTimestamp = o.getString("timestamp")
            // 如果已经有最新的版本, 就不要升级了
            if (poiTypeMap.containsKey(name) &&
                    poiTypeMap[name]!!.timestamp.compareTo(newTimestamp) >= 0) {
                null
            } else {
                mapOf("name" to name, "timestamp" to newTimestamp)
            }
        }.filter {
            it != null
        }.map {
            it!!
        }
        Logger.i("poi types to upgrade: $toBeUpgraded")
        toBeUpgraded.forEach {
            val name = it["name"]
            val response = OkHttpClient().newCall(
                    Request.Builder()
                            .url(makePOITypeUrl(orgCode, name!!))
                            .build()).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected code " + response)
            }
            response.body().byteStream().copyTo(File(dir, "$name.zip"))
        }
        // 删除掉所有不在列表中信息点模板
        val toBeDeleted = {
            set: Set<String> ->
            dir.listFiles {
                it: File ->
                it.isDirectory && it.name !in set
            }
        }(setOf(*toBeUpgraded.map { it["name"]!! }.toTypedArray()))
        dir.listFiles {
            it: File ->
            it.isDirectory
        }.forEach {
            Logger.i("poi type ${it.name} will be deleted")
            it.deleteRecursively()
        }
        tryUnzipSync()
        toBeUpgraded to toBeDeleted
    }
}

private fun unzip(file: File, dir: File) =
    try {
        val zis = ZipInputStream(BufferedInputStream(file.inputStream()))
        val buffer = ByteArray(1024)
        do {
            val ze = zis.nextEntry ?: break
            if (ze.isDirectory) {
                File(dir, ze.name).mkdirs()
                continue
            }
            val out = File(dir, ze.name).apply {
                if (!exists()) {
                    createNewFile()
                }
            }.outputStream()
            do {
                val count = zis.read(buffer)
                if (count == -1) {
                    break
                }
                out.write(buffer, 0, count)
            } while (true)

            out.close();
            zis.closeEntry();
        } while (true)
        zis.close()
        true
    } catch(e: IOException) {
        e.printStackTrace()
        false
    }
