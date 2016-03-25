package com.puzheng.area_investigation.store

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import com.puzheng.area_investigation.DBHelpler
import com.puzheng.area_investigation.model.Area
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat

private fun Cursor.getRow() = Area(getLong(BaseColumns._ID)!!, getString(Area.Model.COL_NAME)!!,
        getDate(Area.Model.COL_CREATED)!!, getDate(Area.Model.COL_UPDATED))

class AreaStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = AreaStore(context)


    }

    fun getCoverImageFile(area: Area): File? = context.openReadableFile("/areas", "${area.id}.png")

    // a list of areas ordered by `created` in descending order
    val areas: Observable<List<Area>>
        get() = Observable.create<List<Area>> {
            val db = DBHelpler(context).readableDatabase
            val cursor = db.query(Area.Model.TABLE_NAME, null, null, null, null, null,
                    "${Area.Model.COL_CREATED} DESC")
            try {
                var rows: List<Area>? = null
                if (cursor.moveToFirst()) {
                    rows = mutableListOf()
                    do {
                        rows.add(cursor.getRow())
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
                format.parse(created),
                if (updated == null) format.parse(created) else format.parse(updated))

        fun fakeAreaImage(id: Long) {
            val outputStream: FileOutputStream = FileOutputStream(context.openWritableFile("/areas", "$id.png"))
            val inputStream = context.assets.open("default_area.png")
            inputStream.transferTo(outputStream)
            outputStream.close()
            inputStream.close()
        }

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
            val id = db.insert(Area.Model.TABLE_NAME, null, Area.Model.makeValues(area))
            fakeAreaImage(id)
        }
        db.close()
        it!!.onCompleted()
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
}
