package com.puzheng.region_investigation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.model.POI
import com.puzheng.region_investigation.model.UploadTask

private const val dbName = "region-investigation.db"
private const val version = 1

class DBHelper(context: Context) : SQLiteOpenHelper(context, dbName, null, version) {

    fun <T> withDb(func: (db: SQLiteDatabase) -> T): T = func(readableDatabase)

    fun <T> withWritableDb(func: (db: SQLiteDatabase) -> T): T = func(writableDatabase)


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Region.Model.CREATE_SQL)
        db?.execSQL(POI.Model.CREATE_SQL)
        db?.execSQL(UploadTask.Model.CREATE_SQL)
    }

}