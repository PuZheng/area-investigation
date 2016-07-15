package com.puzheng.region_investigation.store

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.BaseColumns
import android.text.TextUtils
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.*
import com.puzheng.region_investigation.model.POI
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.model.Region
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import okhttp3.*
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RegionStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = RegionStore(context)
    }

    fun getCoverImageFile(region: Region): File? = context.openReadableFile("/regions", "${region.id}.png")

    // a list of regions ordered by `created` in descending order
    val list: Promise<List<Region>?, Exception>
        get() = task {
            val db = DBHelper(context).readableDatabase
            val cursor = db.query(Region.Model.TABLE_NAME, null, null, null, null, null,
                    "${Region.Model.COL_CREATED} DESC")
            try {
                var rows: List<Region>? = null
                if (cursor.moveToFirst()) {
                    rows = mutableListOf()
                    do {
                        rows.add(cursor.getRegionRow())
                    } while (cursor.moveToNext())
                }
                rows
            } finally {
                cursor.close()
                db.close()
            }
        }

    fun fakeRegion() = POITypeStore.with(context).list then {
        poiTypes ->
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val db = DBHelper(context).writableDatabase

        fun makeRegion(id: Long, created: String, updated: String? = null, synced: String? = null) = Region(id, "region$id",
                mapOf<String, String>(),
                // 任何三个点总能组成一个三角形
                listOf(randomHZLatLng, randomHZLatLng, randomHZLatLng),
                format.parse(created),
                if (updated == null) format.parse(created) else format.parse(updated),
                if (synced == null) null else format.parse(synced))

        fun fakeRegionImage(id: Long) {
            val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/regions", "$id.png"))
            val inputStream = context.assets.open("default_region.png")
            inputStream.transferTo(outputStream)
            outputStream.close()
            inputStream.close()
        }

        val random = Random()
        for (region in listOf(
                makeRegion(1L, "2016-03-08 17:30:31"),
                makeRegion(2L, "2016-03-08 14:32:31", "2016-03-10 12:12:31", "2016-03-10 12:12:31"),
                makeRegion(3L, "2016-03-08 10:32:31"),
                makeRegion(4L, "2016-03-01 17:32:31"),
                makeRegion(5L, "2016-03-01 12:32:31"),
                makeRegion(6L, "2016-01-01 7:32:31", "2016-03-14 14:32:31", "2016-03-14 14:32:31"),
                makeRegion(7L, "2015-09-08 17:32:31"),
                makeRegion(8L, "2015-09-08 10:32:31"),
                makeRegion(9L, "2015-03-02 9:32:31"),
                makeRegion(10L, "2016-03-02 17:32:31"),
                makeRegion(11L, "2016-03-02 12:32:31"),
                makeRegion(12L, "2016-03-02 10:32:31"),
                makeRegion(13L, "2016-03-02 8:32:31")
        )) {
            Logger.v("fake region ${region.name}")
            val regionId = db.insert(Region.Model.TABLE_NAME, null, Region.Model.makeValues(region))
            fakeRegionImage(regionId)
            for (i in 1..2 + Random().nextInt(100)) {
                db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(POI(null,
                        poiTypes!![random.nextInt(poiTypes.size)].name, regionId, randomHZLatLng, Date())))
            }
        }
    }

    fun removeRegions(regions: List<Region>) = task {
        val db = DBHelper(context).writableDatabase
        db.delete(Region.Model.TABLE_NAME, """${BaseColumns._ID} IN (${regions.map { it.id.toString() }.joinToString(",")})""", null)
        regions.forEach {
            with(context).getCoverImageFile(it)?.delete()
            // TODO remove related pois
        }
        MyApplication.eventLogger.log(Level.INFO, "删除重点区域", JSONObject().apply {
            put("type", EventType.DELETE_REGION)
            put("regions", JSONArray().apply {
                regions.forEach {
                    put(JSONObject().apply {
                        put("id", it.id)
                        put("name", it.name)
                    })
                }
            })
        })
    }


    fun create(region: Region, bitmap: Bitmap? = null) = task {
        val db = DBHelper(context).writableDatabase
        val id = db.insert(Region.Model.TABLE_NAME, null, Region.Model.makeValues(region))
        if (bitmap != null) {

            val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/regions", "$id.png"))
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            val inputStream = ByteArrayInputStream(stream.toByteArray())
            inputStream.transferTo(outputStream)
            outputStream.close()
            inputStream.close()
        }
        id
    } then {
        MyApplication.eventLogger.log(Level.INFO, "创建重点区域`${region.name}`", JSONObject().apply {
            put("type", EventType.CREATE_REGION)
            put("id", it)
            put("name", region.name)
        })
        it
    }

    fun get(id: Long) = task {
        getSync(id)
    }

    fun getSync(id: Long) = DBHelper(context).withDb {
        db ->
        try {
            val cursor = db.query(Region.Model.TABLE_NAME, null, "${BaseColumns._ID}=?", arrayOf(id.toString()), null,
                    null, null)
            if (cursor.moveToFirst()) {
                cursor.getRegionRow()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e(e.toString())
            null
        }
    }


    fun updateName(region: Region, name: String) = task {
        val db = DBHelper(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        db.update(Region.Model.TABLE_NAME, ContentValues().apply {
            put(Region.Model.COL_NAME, name)
            put(Region.Model.COL_UPDATED, format.format(Date()))
        },
                "${BaseColumns._ID}=${region.id}", null)
        MyApplication.eventLogger.log(Level.INFO, "修改重点区域`${region.name}`", JSONObject().apply {
            put("type", EventType.UPDATE_REGION)
            put("id", region.id)
            put("fields", JSONObject().apply {
                put("name", JSONObject().apply {
                    put("old", region.name)
                    put("new", name)
                })
            })
        })
    }

    fun updateOutline(region: Region, outline: List<LatLng>, bitmap: Bitmap? = null) = task {
        val db = DBHelper(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                put(Region.Model.COL_OUTLINE, Region.encodeOutline(outline))
                put(Region.Model.COL_UPDATED, format.format(Date()))
            },
                    "${BaseColumns._ID}=${region.id}", null)
            if (bitmap != null) {
                val destFile = context.openWritableFile("/regions", "${region.id}.png")
                destFile.delete()
                val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/regions", "${region.id}.png"))
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                val inputStream = ByteArrayInputStream(stream.toByteArray())
                inputStream.transferTo(outputStream)
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        MyApplication.eventLogger.log(Level.INFO, "修改重点区域`${region.name}`", JSONObject().apply {
            put("type", EventType.UPDATE_REGION)
            put("id", region.id)
            put("fields", JSONObject().apply {
                put("outline", JSONObject().apply {
                    put("old", Region.encodeOutline(region.outline))
                    put("new", Region.encodeOutline(outline))
                })
            })
        })

    }


    fun touch(id: Long) = task {
        val db = DBHelper(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        db.update(Region.Model.TABLE_NAME, ContentValues().apply {
            put(Region.Model.COL_UPDATED, format.format(Date()))
        },
                "${BaseColumns._ID}=$id", null)
    }

    fun uniqueName(name: String) = task {
        val db = DBHelper(context).readableDatabase
        val cursor = db.query(Region.Model.TABLE_NAME, null, "${Region.Model.COL_NAME}=?", arrayOf(name), null, null,
                null)
        cursor.count == 0
    }

    val zipDir = File(Environment.getExternalStoragePublicDirectory(MyApplication.context.packageName),
            ".cache").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun generateZipSync(region: Region) {
        try {
            val jsonObject = JSONObject().apply {
                region.jsonizeSync(this)
            }
            val zipFile = File(zipDir, "${region.id}.zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).apply {
                putNextEntry(ZipEntry("json/region.json"))
                write(jsonObject.toString().toByteArray())
                closeEntry()
                val pois = region.poiListSync
                pois?.forEach {
                    poi ->
                    addDir(poi.dir, "json/pois/" + poi.id)
                }

                putNextEntry(ZipEntry("csv/region.csv"))
                write(region.csvString.toByteArray())
                closeEntry()

                // 按各个信息点分类生成csv
                val poiTypeMap = POITypeStore.with(context).listSync.let {
                    if (it != null) {
                        mapOf(*it.map {
                            it.name to it
                        }.toTypedArray())
                    } else {
                        mapOf<String, POIType>()
                    }
                }
                pois?.groupBy { it.poiTypeName }?.forEach {
                    val typeName = it.key
                    val poiType = poiTypeMap[typeName]
                    if (poiType != null) {
                        addPoiTypeCSV(this, poiType, it.value)
                    }

                }

                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    // 生成某类型的信息点CSV文件
    fun addPoiTypeCSV(zos: ZipOutputStream, poiType: POIType, pois: List<POI>) {
        // 将需要打包的图片和视频记录下来， 生成csv文件后，加入zip包
        val assets = mutableListOf<Pair<String, File>>()
        zos.putNextEntry(ZipEntry("csv/${poiType.name}.csv"))
        // headers
        zos.write((poiType.fields.map { it.name } +
                listOf("经度", "纬度", "采集人", "采集时间", "更新时间",
                        "采集单位名称", "采集单位编码", "录入人"))
                .joinToString(",").toByteArray())
        zos.write("\n".toByteArray())
        // each row
        pois.forEach {
            poi ->
            val jo = if (poi.dataFile.exists()) {
                JSONObject(poi.dataFile.readText())
            } else {
                JSONObject()
            }
            val fields = mutableListOf<String>()
            poiType.fields.forEach {
                when (it.type) {
                    POIType.FieldType.STRING,
                    POIType.FieldType.TEXT ->
                        fields.add(CSVUtil.quote(jo.getString(it.name)))
                    POIType.FieldType.IMAGES -> {
                        val fileNames = mutableListOf<String>()
                        try {
                            val ja = jo.getJSONArray(it.name)
                            if (ja != null && ja.length() > 0) {
                                var i = 0
                                while (i < ja.length()) {
                                    val path = ja.getString(i)
                                    val suffix = path.split(".").last()
                                    val fileName = "${uniqueId()}.$suffix"
                                    fileNames.add(fileName)
                                    assets.add(fileName to File(poi.dir, path))
                                    ++i
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        fields.add(CSVUtil.quote(fileNames.joinToString(";")))
                    }
                    POIType.FieldType.VIDEO -> {
                        try {
                            val path = jo.getString(it.name)
                            if (!TextUtils.isEmpty(path)) {
                                val suffix = path.split(".").last()
                                val fileName = "${uniqueId()}.$suffix"
                                assets.add(fileName to File(poi.dir, path))
                                fields.add(CSVUtil.quote(fileName))
                            } else {
                                fields.add("")
                            }
                        } catch (e: JSONException) {
                            fields.add("")
                        }
                    }

                }
            }
            // 经度
            fields.add(poi.latLng.longitude.toString())
            // 纬度
            fields.add(poi.latLng.latitude.toString())
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            AccountStore.with(context).account.let {
                // 采集人
                fields.add(CSVUtil.quote(it!!.username))
                // 采集时间
                fields.add(CSVUtil.quote(sdf.format(poi.created)))
                // 更新时间
                fields.add(CSVUtil.quote(if (poi.updated != null) {
                    sdf.format(poi.updated)
                } else {
                    ""
                }))
                // 采集单位名称
                fields.add(CSVUtil.quote(it.orgName))
                // 采集单位编码
                fields.add(CSVUtil.quote(it.orgCode))
                // 录入人
                fields.add("")
            }
            zos.write((fields.joinToString(CSVUtil.fieldSep) + CSVUtil.lineSep).toByteArray())
        }
        zos.closeEntry()
        val buf = ByteArray(4096)
        assets.forEach {
            zos.putNextEntry(ZipEntry("csv/${it.first}"))
            try {
                BufferedInputStream(FileInputStream(it.second)).let {
                    src ->
                    while (true) {
                        val count = src.read(buf, 0, buf.size)
                        if (count == -1) {
                            break
                        }
                        zos.write(buf, 0, count)
                    }
                    src.close()
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
            zos.closeEntry()
        }
    }

    private fun ZipOutputStream.addDir(dir: File, prefix: String = "") {
        val buf = ByteArray(4096)
        dir.listFiles().forEach {
            when {
                it.isFile -> {
                    putNextEntry(ZipEntry(File(prefix, it.relativeTo(dir).path).path.apply {
                        Logger.v("put entry $this into zip")
                    }))
                    try {
                        BufferedInputStream(FileInputStream(it)).let {
                            src ->
                            while (true) {
                                val count = src.read(buf, 0, buf.size)
                                if (count == -1) {
                                    break
                                }
                                write(buf, 0, count)
                            }
                            src.close()
                        }
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                    closeEntry()
                }
                it.isDirectory -> {
                    addDir(File(dir, it.name), File(prefix, it.name).path)
                }
            }
        }
    }

    fun uploadSync(region: Region, onProgress: (sent: Long) -> Unit) {
        val zipFile = File(zipDir, "${region.id}.zip")
        var sent = 0L
        val accountStore = AccountStore.with(context)
        val progressingRequestBody = object : RequestBody() {
            override fun contentType(): MediaType? = MediaType.parse("application/zip")

            override fun contentLength() = zipFile.length()

            override fun writeTo(sink: BufferedSink) {
                val buf = ByteArray(4096)
                val src = BufferedInputStream(FileInputStream(zipFile))
                while (true) {
                    val count = src.read(buf, 0, buf.size)
                    if (count == -1) {
                        break
                    }
                    sent += count
                    onProgress(sent)
                    sink.write(buf, 0, count)
                }
                src.close()
            }
        }
        val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", accountStore.account?.username)
                .addFormDataPart("orgCode", accountStore.account?.orgCode)
                .addFormDataPart("file", region.name + ".zip", progressingRequestBody).build()
        val response = OkHttpClient().newCall(
                Request.Builder()
                        .url(Uri.parse(ConfigStore.with(context).backend).buildUpon().appendEncodedPath("region/object").build().toString())
                        .post(body)
                        .build()).execute()
        if (!response.isSuccessful) {
            throw IOException("Unexpected code " + response)
        }
        response.body().close()
        MyApplication.eventLogger.log(Level.INFO, "上传重点区域", JSONObject().apply {
            put("type", EventType.UPLOAD_REGION)
            put("region", JSONObject().apply {
                put("id", region.id)
                put("name", region.name)
            })
        })
    }
}


