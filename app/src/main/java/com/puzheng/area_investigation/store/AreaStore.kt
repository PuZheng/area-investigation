package com.puzheng.area_investigation.store

import com.puzheng.area_investigation.model.Area
import rx.Observable
import rx.Subscriber
import java.text.SimpleDateFormat


// a list of areas ordered by `created` in descending order
val areas: Observable<List<Area>>
    get()  {
        val format = SimpleDateFormat("yyyy-MM-dd")
        return Observable.create<List<Area>> { t ->
            Thread.sleep(2000)
            t!!.onNext(null)
//            t!!.onNext(listOf(
//                    Area(1, "area1", format.parse("2015-12-01")),
//                    Area(2, "area2", format.parse("2015-12-01")),
//                    Area(3, "area3", format.parse("2015-09-01")),
//                    Area(4, "area4", format.parse("2015-09-01")),
//                    Area(5, "area5", format.parse("2015-09-01")),
//                    Area(6, "area6", format.parse("2015-02-01"))
//            ))
            t!!.onCompleted()
        }

    }