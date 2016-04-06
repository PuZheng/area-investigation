package com.puzheng.area_investigation.store

import android.content.Context
import com.puzheng.area_investigation.DBHelpler
import com.puzheng.area_investigation.model.POI
import nl.komponents.kovenant.task

class POIStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POIStore(context)
    }

    fun create(poi: POI) = task {
        val db = DBHelpler(context).writableDatabase
        try {
            db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(poi))
        } finally {
            db.close()
        }
    }
}