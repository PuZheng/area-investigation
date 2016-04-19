package com.puzheng.region_investigation

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.LocationSource
import com.orhanobut.logger.Logger
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred

class LocateMyselfHelper(val context: Context, val onLocationChangeListener: LocationSource.OnLocationChangedListener) {

    fun locate(): Promise<AMapLocation, Exception> {
        //初始化定位
        var d = deferred<AMapLocation, Exception>()
        val locationClient = AMapLocationClient(context)
        //设置定位回调监听
        locationClient.setLocationListener({
            if (it?.errorCode == 0) {
                Logger.e(it.toStr())
                onLocationChangeListener.onLocationChanged(it)
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
        locationClient.setLocationOption(locationOption);
        //启动定位
        locationClient.startLocation()
        return d.promise
    }
}


