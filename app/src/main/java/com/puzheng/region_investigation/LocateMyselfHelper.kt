package com.puzheng.region_investigation

import android.content.Context
import android.content.SharedPreferences
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred

class LocateMyselfHelper(val context: Context, val onLocationChangeListener: LocationSource.OnLocationChangedListener) {

    companion object {
        private const val LNG = "LNG"
        private const val LAT = "LAT"
    }

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences(context.getString(R.string.preference_last_location),
                Context.MODE_PRIVATE)
    }

    val lastLocation: LatLng? by lazy {
        val (lat, lng) = {
            sharedPref: SharedPreferences ->
            sharedPref.getFloat(LAT, -1F).toDouble() to sharedPref.getFloat(LNG, -1F).toDouble()
        }(sharedPref)
        if (lat.toInt() == -1 || lng.toInt() == -1) {
            Logger.e("无法获取上次的坐标")
            null
        } else {
            Logger.e("无法定位，使用最近的坐标")
            LatLng(lat, lng)
        }
    }

    fun locate(): Promise<AMapLocation, Exception> {
        //初始化定位
        var d = deferred<AMapLocation, Exception>()
        val locationClient = AMapLocationClient(context)
        //设置定位回调监听
        locationClient.setLocationListener({
            if (it?.errorCode == 0) {
                Logger.e(it.toStr())
                onLocationChangeListener.onLocationChanged(it)
                sharedPref.edit().apply {
                    putFloat(LAT, it.latitude.toFloat())
                    putFloat(LNG, it.latitude.toFloat())
                    commit()
                }
                d.resolve(it)
            } else {
                Logger.e("定位失败, ${it.errorCode}: ${it.errorInfo}");
                d.reject(Exception("${it.errorCode}: ${it.errorInfo}"))
            }
        })
        //初始化定位参数
        val locationOption = AMapLocationClientOption()
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;
        locationOption.isOnceLocation = true // 只需要定位一次
        locationClient.setLocationOption(locationOption)
        //启动定位
        locationClient.startLocation()
        return d.promise
    }
}


