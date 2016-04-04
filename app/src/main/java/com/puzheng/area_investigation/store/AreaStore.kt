package com.puzheng.area_investigation.store

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.BaseColumns
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.*
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.model.POI
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.observables.BlockingObservable
import rx.schedulers.Schedulers
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AreaStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = AreaStore(context)


    }

    fun getCoverImageFile(areaId: Long): File? = context.openReadableFile("/areas", "$areaId.png")
    fun getCoverImageFile(area: Area): File? = context.openReadableFile("/areas", "${area.id}.png")

    // a list of areas ordered by `created` in descending order
    val list: Observable<List<Area>>
        get() = Observable.create<List<Area>> {
            val db = DBHelpler(context).readableDatabase
            val cursor = db.query(Area.Model.TABLE_NAME, null, null, null, null, null,
                    "${Area.Model.COL_CREATED} DESC")
            try {
                var rows: List<Area>? = null
                if (cursor.moveToFirst()) {
                    rows = mutableListOf()
                    do {
                        rows.add(cursor.getAreaRow())
                    } while (cursor.moveToNext())
                }
                it!!.onNext(rows)
                it!!.onCompleted()
            } finally {
                cursor.close()
                db.close()
            }
        }.subscribeOn(Schedulers.computation())

    fun fakeAreas() = Observable.create<Void> {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val db = DBHelpler(context).writableDatabase

        fun fakeArea(id: Long, created: String, updated: String? = null) = Area(id, "area$id",
                // 任何三个点总能组成一个三角形
                listOf(randomHZLatLng, randomHZLatLng, randomHZLatLng),
                format.parse(created),
                if (updated == null) format.parse(created) else format.parse(updated))

        fun fakeAreaImage(id: Long) {
            val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/areas", "$id.png"))
            val inputStream = context.assets.open("default_area.png")
            inputStream.transferTo(outputStream)
            outputStream.close()
            inputStream.close()
        }
        POITypeStore.with(context).list.observeOn(Schedulers.immediate()).subscribe {
            poiTypes ->
            val random = Random()
            for (area in listOf(
                    fakeArea(1L, "2016-03-08 17:30:31"),
                    fakeArea(2L, "2016-03-08 14:32:31", "2016-03-10 12:12:31"),
                    fakeArea(3L, "2016-03-08 10:32:31"),
                    fakeArea(4L, "2016-03-01 17:32:31"),
                    fakeArea(5L, "2016-03-01 12:32:31"),
                    fakeArea(6L, "2016-01-01 7:32:31", "2016-03-14 14:32:31"),
                    fakeArea(7L, "2015-09-08 17:32:31"),
                    fakeArea(8L, "2015-09-08 10:32:31"),
                    fakeArea(9L, "2015-03-02 9:32:31"),
                    fakeArea(10L, "2016-03-02 17:32:31"),
                    fakeArea(11L, "2016-03-02 12:32:31"),
                    fakeArea(12L, "2016-03-02 10:32:31"),
                    fakeArea(13L, "2016-03-02 8:32:31")
            )) {
                val areaId = db.insert(Area.Model.TABLE_NAME, null, Area.Model.makeValues(area))
                Logger.v("create area: ${area.name}")
                fakeAreaImage(areaId)
                for (i in 1..2 + Random().nextInt(20)) {
                    db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(POI(null,
                            poiTypes[random.nextInt(poiTypes.size)].uuid, areaId, randomHZLatLng, Date())))
                }
            }
            db.close()
            it!!.onNext(null)
        }

    }.subscribeOn(Schedulers.computation())

    fun removeAreas(areas: List<Area>) = Observable.create<Void> {
        val db = DBHelpler(context).writableDatabase
        try {
            db.delete(Area.Model.TABLE_NAME, """${BaseColumns._ID} IN (${areas.map { it.id.toString() }.joinToString(",")})""", null)

        } finally {
            db.close()
        }
        areas.forEach {
            AreaStore.with(context).getCoverImageFile(it)?.delete()
        }
        it!!.onCompleted()
    }.subscribeOn(Schedulers.computation())


    fun createArea(area: Area, bitmap: Bitmap? = null) = Observable.create<Long> {
        val db = DBHelpler(context).writableDatabase
        try {
            val id = db.insert(Area.Model.TABLE_NAME, null, Area.Model.makeValues(area))
            if (bitmap != null) {

                val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/areas", "$id.png"))
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                val inputStream = ByteArrayInputStream(stream.toByteArray())
                inputStream.transferTo(outputStream)
                outputStream.close()
                inputStream.close()
            }
            it!!.onNext(id)
        } finally {
            db.close()
        }
    }.subscribeOn(Schedulers.computation())

    fun updateAreaName(id: Long, name: String) = Observable.create<Void> {
        val db = DBHelpler(context).writableDatabase
        try {
            db.update(Area.Model.TABLE_NAME, ContentValues().apply { put(Area.Model.COL_NAME, name) },
                    "${BaseColumns._ID}=$id", null)
            it!!.onNext(null)
        } finally {
            db.close()
        }
    }.subscribeOn(Schedulers.computation())

    fun updateAreaOutline(id: Long, outline: List<LatLng>, bitmap: Bitmap? = null) = Observable.create<Void> {
        val db = DBHelpler(context).writableDatabase
        try {
            db.update(Area.Model.TABLE_NAME, ContentValues().apply { put(Area.Model.COL_OUTLINE, Area.encodeOutline(outline)) },
                    "${BaseColumns._ID}=$id", null)
            if (bitmap != null) {
                val destFile = context.openWritableFile("/areas", "$id.png")
                destFile.delete()
                val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/areas", "$id.png"))
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                val inputStream = ByteArrayInputStream(stream.toByteArray())
                inputStream.transferTo(outputStream)
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            }
            it!!.onNext(null)
        } finally {
            db.close()
        }
    }.subscribeOn(Schedulers.computation())

    fun getPOIList(area: Area) = Observable.create<List<POI>> {
        val db = DBHelpler(context).readableDatabase
        val cursor = db.query(POI.Model.TABLE_NAME, null, "area_id=?", arrayOf(area.id.toString()), null, null,
                "${Area.Model.COL_CREATED} DESC")
        try {
            var rows: List<POI>? = null
            if (cursor.moveToFirst()) {
                rows = mutableListOf()
                do {
                    rows.add(cursor.getPOIRow())
                } while (cursor.moveToNext())
            }
            it!!.onNext(rows)
            it!!.onCompleted()
        } finally {
            cursor.close()
            db.close()
        }
    }.subscribeOn(Schedulers.computation())
}
