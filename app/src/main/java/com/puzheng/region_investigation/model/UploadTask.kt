package com.puzheng.region_investigation.model

import android.content.ContentValues
import android.provider.BaseColumns
import com.puzheng.region_investigation.DBHelper
import com.puzheng.region_investigation.MyApplication
import java.text.SimpleDateFormat
import java.util.*

data class UploadTask(val id: Long, val regionId: Long, val createdAt: Date, val finishedAt: Date?,
                      var region: Region? = null) {

    companion object {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }

    var isFinished: Boolean
        get() = finishedAt != null
        set(value) {
            DBHelper(MyApplication.context).withWritableDb {
                db ->
                try {
                    db.update(Model.TABLE_NAME, ContentValues().apply {
                        put(Model.COL_FINISHED_AT, if (value) format.format(Date()) else null)
                    }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    class Model {
        companion object {

            const val TABLE_NAME = "upload_task"
            const val COL_REGION_ID = "region_id"
            const val COL_CREATED_AT = "created"
            const val COL_FINISHED_AT = "finished_at"
            const val CREATE_SQL = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_REGION_ID LONG NOT NULL,
                        $COL_CREATED_AT TEXT NOT NULL,
                        $COL_FINISHED_AT TEXT
                    )
                """
        }
    }
}