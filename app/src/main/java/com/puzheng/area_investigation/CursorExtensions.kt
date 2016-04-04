package com.puzheng.area_investigation

import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.*


fun Cursor.getString(colName: String): String? {
    val index = getColumnIndexOrThrow(colName)
    return if (isNull(index)) null else getString(index)
}

fun Cursor.getLong(colName: String): Long? {
    val index = getColumnIndexOrThrow(colName)
    return if (isNull(index)) null else getLong(index)
}

fun Cursor.getDate(colName: String): Date? {
    val index = getColumnIndexOrThrow(colName)
    return if (isNull(index)) null else {
        val s = getString(colName)
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s)
    }
}