package com.puzheng.region_investigation

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolygonOptions
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.RegionStore
import nl.komponents.kovenant.ui.successUi

class ConfirmSaveRegionOutlineDialog(val region: Region, val afterSaved: () -> Unit) : AppCompatDialogFragment() {
    private val markerBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.marker),
                (8 * pixelsPerDp).toInt(),
                (8 * pixelsPerDp).toInt(),
                false
        )
    }

    override fun onStart() {
        super.onStart()
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
            map.map.getMapScreenShot(object: AMap.OnMapScreenShotListener {
                override fun onMapScreenShot(p0: Bitmap?) {
                    RegionStore.with(activity).updateOutline(region, region.outline, p0) successUi {
                        afterSaved()
                        if (activity != null) {
                            activity.toast(R.string.outline_saved)
                        }
                        dialog.dismiss()
                    }
                }

                override fun onMapScreenShot(p0: Bitmap?, p1: Int) {
                }
            })
        })
    }

    lateinit private var map: MapView

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val contentView = activity.layoutInflater.inflate(R.layout.dialog_confirm_save_region_outline, null, false)
        map = contentView.findViewById(R.id.map) as MapView
        map.onCreate(savedInstanceState)
        // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
        map.map.setOnMapLoadedListener {
            map.map.apply {
                moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.Builder().apply {
                                    region.outline.forEach { include(it) }
                                }.build(), (8 * pixelsPerDp).toInt()))
                addPolygon(PolygonOptions()
                        .add(*region.outline.toTypedArray())
                        .fillColor(ContextCompat.getColor(activity, R.color.colorOutlinePolygon))
                        .strokeWidth((1 * pixelsPerDp).toFloat())
                        .strokeColor(ContextCompat.getColor(activity, R.color.colorOutlinePolyline)))
                region.outline.forEach {
                    addMarker(MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            .position(it)
                            .anchor(0.5F, 0.5F))
                }
                uiSettings.setAllGesturesEnabled(false)
                uiSettings.isScaleControlsEnabled = false
                uiSettings.isZoomControlsEnabled = false
            }
        }
        // 注意，不能在positive button的响应事件中直接截屏， 因为截屏是异步的，而若这里设置callback, 点击positive button， 会先关闭
        // 对话框(安卓就是这么设计的)，这样导致截屏的时候，对话框已经不存在了，所以要对positive button单独设置click事件处理器
        return AlertDialog.Builder(activity).setView(contentView).setTitle(R.string.confirm_save_region_outline)
                .setPositiveButton(R.string.confirm, null).setNegativeButton(R.string.cancel, null).create()
    }
}

