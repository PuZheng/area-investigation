package com.puzheng.region_investigation.store

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import com.puzheng.region_investigation.DBHelper
import com.puzheng.region_investigation.MyApplication
import com.puzheng.region_investigation.getDate
import com.puzheng.region_investigation.getLong
import com.puzheng.region_investigation.model.UploadTask
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.util.*

private fun Cursor.getUploadTask(): UploadTask {
    val Model = UploadTask.Model
    return UploadTask(
            getLong(BaseColumns._ID)!!,
            getLong(Model.COL_REGION_ID)!!,
            getDate(Model.COL_CREATED_AT, UploadTask.format)!!,
            getDate(Model.COL_FINISHED_AT, UploadTask.format))
}

class UploadTaskStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = UploadTaskStore(context)
    }

    private val dbHelper: DBHelper by lazy {
        DBHelper(context)
    }

    /**
     * 正在上传中的列表
     */
    val list: Promise<List<UploadTask>?, Exception>
        get() = task {
            dbHelper.withDb {
                db ->
                val cursor = db.query(UploadTask.Model.TABLE_NAME, null,
                        "${UploadTask.Model.COL_FINISHED_AT} is null", null, null, null, null, null)
                try {
                    var rows: List<UploadTask?>? = null
                    if (cursor.moveToFirst()) {
                        rows = mutableListOf()
                        do {
                            rows.add(cursor.getUploadTask())
                        } while (cursor.moveToNext())
                    }
                    rows?.filter {
                        it != null
                    }?.map {
                        it!!
                    }
                } finally {
                    cursor.close()
                    db.close()
                }
            }
        }

    fun getByRegionIdSync(regionId: Long) =
        dbHelper.withDb {
            db ->
            val cursor = db.query(UploadTask.Model.TABLE_NAME, null, "${UploadTask.Model.COL_REGION_ID}=?",
                    arrayOf(regionId.toString()), null, null, null, null)
            if (cursor.moveToFirst()) {
                cursor.getUploadTask()
            } else {
                null
            }
        }

    fun newSync(regionId: Long): UploadTask =
        dbHelper.withWritableDb {
            db ->
            val id = db.insert(UploadTask.Model.TABLE_NAME, null, ContentValues().apply {
                put(UploadTask.Model.COL_REGION_ID, regionId)
                put(UploadTask.Model.COL_CREATED_AT, UploadTask.format.format(Date()))
            })
            UploadTaskStore.with(MyApplication.context).getSync(id)!!
        }

    private fun getSync(id: Long): UploadTask? =
        dbHelper.withDb {
            db ->
            val cursor = db.query(UploadTask.Model.TABLE_NAME, null, "${BaseColumns._ID}=?",
                    arrayOf(id.toString()), null, null, null, null)
            if (cursor.moveToFirst()) {
                cursor.getUploadTask()
            } else {
                null
            }
        }
}

