package com.puzheng.area_investigation.store

import android.content.Context
import com.puzheng.area_investigation.DBHelpler
import com.puzheng.area_investigation.model.POI
import rx.Observable
import rx.schedulers.Schedulers

class POIStore private constructor(val context: Context) {

    companion object {
        fun with(context: Context) = POIStore(context)
    }

    fun create(poi: POI) = Observable.create<Long> {
        val db = DBHelpler(context).writableDatabase
        try {
            val id = db.insert(POI.Model.TABLE_NAME, null, POI.Model.makeValues(poi))
            it!!.onNext(id)
        } finally {
            db.close()
        }
    }.subscribeOn(Schedulers.computation())
}