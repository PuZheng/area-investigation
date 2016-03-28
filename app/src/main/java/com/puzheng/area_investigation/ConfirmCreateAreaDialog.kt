package com.puzheng.area_investigation

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.*
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.store.AreaStore
import kotlinx.android.synthetic.main.dialog_create_area_successfully.view.*
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class ConfirmCreateAreaDialog(val name: String, val latLatList: List<LatLng>) : DialogFragment() {
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
            map.map.getMapScreenShot {
                Logger.v("${it.width}, ${it.height}")
                AreaStore.with(context).createArea(Area(null, name, Date()), it)
                        .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    Toast.makeText(context, R.string.create_area_successfully, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    activity.setResult(Activity.RESULT_OK)
                    activity.finish()
                }
            }
        })
    }

    lateinit private var map: MapView

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val contentView = activity.layoutInflater.inflate(R.layout.dialog_create_area_successfully, null, false)
        map = contentView.map
        map.onCreate(savedInstanceState)
        contentView.area_name.text = name
        // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
        map.map.setOnMapLoadedListener {
            map.map.apply {
                moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.Builder().apply {
                                    latLatList.forEach { include(it) }
                                }.build(), (8 * pixelsPerDp).toInt()))
                addPolygon(PolygonOptions()
                        .add(*latLatList.toTypedArray())
                        .fillColor(ContextCompat.getColor(activity, R.color.colorAccentLighter))
                        .strokeWidth((1 * pixelsPerDp).toFloat())
                        .strokeColor(ContextCompat.getColor(activity, R.color.colorAccent)))
                latLatList.forEach {
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
        return AlertDialog.Builder(activity).setView(contentView).setTitle(R.string.confirm_create_area)
                .setPositiveButton(R.string.confirm, null).setNegativeButton(R.string.cancel, null).create()
    }
}

