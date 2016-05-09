package com.puzheng.region_investigation

import android.provider.BaseColumns

class UploadTaskModel {
    companion object {
        const val TABLE_NAME = "upload_task"
        const val COL_REGION_ID = "region_id"
        const val COL_CREATED = "created"
        const val CREATE_SQL = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_REGION_ID LONG NOT NULL,
                        $COL_CREATED TEXT NOT NULL
                    )
                """
    }

}