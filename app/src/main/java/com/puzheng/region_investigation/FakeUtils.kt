package com.puzheng.region_investigation

import com.amap.api.maps.model.LatLng
import java.util.*

object HANG_ZHOU {
    val ne = LatLng(30.311246, 120.18631)
    val sw = LatLng(30.15344, 120.076447)
}


val randomHZLatLng: LatLng
    get() = LatLng(HANG_ZHOU.sw.latitude + (HANG_ZHOU.ne.latitude - HANG_ZHOU.sw.latitude) * Math.random(),
            HANG_ZHOU.sw.longitude + (HANG_ZHOU.ne.longitude - HANG_ZHOU.sw.longitude) * Math.random())

