package com.puzheng.region_investigation.store

import android.content.ContentValues
import android.content.Context
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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

        fun fakeRegion(id: Long, created: String, updated: String? = null) = Region(id, "region$id",
                // 任何三个点总能组成一个三角形
                listOf(randomHZLatLng, randomHZLatLng, randomHZLatLng),
                format.parse(created),
                if (updated == null) format.parse(created) else format.parse(updated))

        fun fakeRegionImage(id: Long) {
            val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/regions", "$id.png"))
            val inputStream = context.assets.open("default_region.png")
            inputStream.transferTo(outputStream)
            outputStream.close()
            inputStream.close()
        }


        val random = Random()
        for (region in listOf(
                fakeRegion(1L, "2016-03-08 17:30:31"),
                fakeRegion(2L, "2016-03-08 14:32:31", "2016-03-10 12:12:31"),
                fakeRegion(3L, "2016-03-08 10:32:31"),
                fakeRegion(4L, "2016-03-01 17:32:31"),
                fakeRegion(5L, "2016-03-01 12:32:31"),
                fakeRegion(6L, "2016-01-01 7:32:31", "2016-03-14 14:32:31"),
                fakeRegion(7L, "2015-09-08 17:32:31"),
                fakeRegion(8L, "2015-09-08 10:32:31"),
                fakeRegion(9L, "2015-03-02 9:32:31"),
                fakeRegion(10L, "2016-03-02 17:32:31"),
                fakeRegion(11L, "2016-03-02 12:32:31"),
                fakeRegion(12L, "2016-03-02 10:32:31"),
                fakeRegion(13L, "2016-03-02 8:32:31")
        )) {
            val regionId = db.insert(Region.Model.TABLE_NAME, null, Region.Model.makeValues(region))
            Logger.v("create region: ${region.name}")
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
        }
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
    }

    fun get(id: Long) = task {
        val db = DBHelpler(context).readableDatabase
        try {
            val cursor = db.query(Region.Model.TABLE_NAME, null, "${BaseColumns._ID}=?", arrayOf(id.toString()), null,
                    null, null)
            if (cursor.moveToFirst()) {
                cursor.getRegionRow()
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e(e.toString())
            null
        } finally {
            db.close()
        }

    }

    fun updateName(id: Long, name: String) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply { put(Region.Model.COL_NAME, name) },
                    "${BaseColumns._ID}=$id", null)
        } finally {
            db.close()
        }
    }

    fun updateOutline(id: Long, outline: List<LatLng>, bitmap: Bitmap? = null) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.update(Region.Model.TABLE_NAME, ContentValues().apply { put(Region.Model.COL_OUTLINE, Region.encodeOutline(outline)) },
                    "${BaseColumns._ID}=$id", null)
            if (bitmap != null) {
                val destFile = context.openWritableFile("/regions", "$id.png")
                destFile.delete()
                val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/regions", "$id.png"))
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
}
