package com.puzheng.region_investigation.store

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.*
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.model.POI
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level

class RegionStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = RegionStore(context)
    }

    fun getCoverImageFile(region: Region): File? = context.openReadableFile("/regions", "${region.id}.png")

    // a list of regions ordered by `created` in descending order
    val list: Promise<List<Region>?, Exception>
        get() = task {
            val db = DBHelpler(context).readableDatabase
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
        val db = DBHelpler(context).writableDatabase

        fun makeRegion(id: Long, created: String, updated: String? = null, synced: String? = null) = Region(id, "region$id",
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
                        poiTypes!![random.nextInt(poiTypes.size)].uuid, regionId, randomHZLatLng, Date())))
            }
        }
        db.close()
    }

    fun removeRegions(regions: List<Region>) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.delete(Region.Model.TABLE_NAME, """${BaseColumns._ID} IN (${regions.map { it.id.toString() }.joinToString(",")})""", null)
        } finally {
            db.close()
        }
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
        val db = DBHelpler(context).writableDatabase
        try {
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
        } finally {
            db.close()
        }
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

    fun getSync(id: Long) = DBHelpler(context).withDb {
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
        } finally {
            db.close()
        }
    }


    fun updateName(region: Region, name: String) = task {
        val db = DBHelpler(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                put(Region.Model.COL_NAME, name)
                put(Region.Model.COL_UPDATED, format.format(Date()))
            },
                    "${BaseColumns._ID}=${region.id}", null)
        } finally {
            db.close()
        }
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
        val db = DBHelpler(context).writableDatabase
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
        } finally {
            db.close()
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

    fun getPOIList(region: Region) = task {
        val db = DBHelpler(context).readableDatabase

        try {
            val cursor = db.query(POI.Model.TABLE_NAME, null, "${POI.Model.COL_REGION_ID}=?", arrayOf(region.id.toString()),
                    null, null, null)
            var rows: List<POI>? = null
            if (cursor.moveToFirst()) {
                rows = mutableListOf()
                do {
                    rows.add(cursor.getPOIRow())
                } while (cursor.moveToNext())
            }
            cursor.close()
            rows
        } catch (e: Exception) {
            Logger.e(e.toString())
            null
        } finally {
            db.close()
        }
    }

    fun touch(id: Long) = task {
        val db = DBHelpler(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                put(Region.Model.COL_UPDATED, format.format(Date()))
            },
                    "${BaseColumns._ID}=$id", null)
        } finally {
            db.close()
        }
    }

    fun sync(ids: List<Long>) = task {
        val db = DBHelpler(context).writableDatabase
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                put(Region.Model.COL_SYNCED, format.format(Date()))
            },
                    "${BaseColumns._ID} in (${ids.joinToString()})", null)
        } finally {
            db.close()
        }
    }

    fun uniqueName(name: String) = task {
        val db = DBHelpler(context).readableDatabase
        try {
            val cursor = db.query(Region.Model.TABLE_NAME, null, "${Region.Model.COL_NAME}=?", arrayOf(name), null, null,
                    null)
            cursor.count == 0
        } finally {
            db.close()
        }
    }
}
