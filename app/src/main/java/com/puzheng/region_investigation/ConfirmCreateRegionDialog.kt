package com.puzheng.region_investigation

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.widget.Toast
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.dialog_create_region_successfully.view.*
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import java.util.*

class ConfirmCreateRegionDialog(val name: String, val latLngList: List<LatLng>) : AppCompatDialogFragment() {
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
                    Logger.v("${it.width}, ${it.height}")
                    val regionStore = RegionStore.with(context)
                    regionStore.create(Region(null, name, latLngList, Date()), p0) successUi {
                        Toast.makeText(context, R.string.create_region_successfully, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } then {
                        regionStore.get(it) successUi {
                            val intent = Intent(context, EditRegionActivity::class.java)
                            intent.putExtra(RegionListActivity.TAG_REGION, it)
                            startActivity(intent)
                            activity.finish()
                        }
                    }
                }

                override fun onMapScreenShot(p0: Bitmap?, p1: Int) {
                }
            })
        })
    }

    lateinit private var map: MapView

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val contentView = activity.layoutInflater.inflate(R.layout.dialog_create_region_successfully, null, false)
        map = contentView.map
        map.onCreate(savedInstanceState)
        contentView.name.text = name
        // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
        map.map.setOnMapLoadedListener {
            map.map.apply {
                moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.Builder().apply {
                                    latLngList.forEach { include(it) }
                                }.build(), (8 * pixelsPerDp).toInt()))
                addPolygon(PolygonOptions()
                        .add(*latLngList.toTypedArray())
                        .fillColor(ContextCompat.getColor(activity, R.color.colorOutlinePolygon))
                        .strokeWidth((1 * pixelsPerDp).toFloat())
                        .strokeColor(ContextCompat.getColor(activity, R.color.colorOutlinePolyline)))
                latLngList.forEach {
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
        return AlertDialog.Builder(activity).setView(contentView).setTitle(R.string.confirm_create_region)
                .setPositiveButton(R.string.confirm, null).setNegativeButton(R.string.cancel, null).create()
    }
}

