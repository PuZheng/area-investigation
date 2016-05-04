package com.puzheng.region_investigation

import com.amap.api.maps.model.LatLng

const val EQUATOR_RADIUS = 6378137.0

private fun LatLng.truncate(): LatLng {
    var lng = longitude
    var lat = latitude
    if (lng > 180.0) {
        lng = 180.0;
    } else if (lng < -180.0) {
        lng = -180.0;
    }
    if (lat > 90.0) {
        lat = 90.0;
    } else if (lat < -90.0) {
        lat = -90.0;
    }
    return LatLng(lat, lng)
}

private fun radians(degrees: Double) = degrees * Math.PI / 180


/**
 * Returns the Spherical Mercator (x, y) in meters
 */
val LatLng.xy: Pair<Double, Double>
    get() {
        val latLng = this.truncate()
        return Pair(
                EQUATOR_RADIUS * Math.log(Math.tan(Math.PI / 4 + radians(latLng.latitude) / 2)),
                EQUATOR_RADIUS * radians(latLng.longitude)
        )
    }
