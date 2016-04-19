package com.puzheng.region_investigation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.model.POI

val dbName = "region-investigation.db"
val version = 1

class DBHelpler(context: Context) : SQLiteOpenHelper(context, dbName, null, version) {

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(Region.Model.CREATE_SQL)
        db?.execSQL(POI.Model.CREATE_SQL)
    }

}