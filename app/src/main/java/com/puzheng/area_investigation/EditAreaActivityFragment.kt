package com.puzheng.area_investigation

import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolygonOptions
import com.puzheng.area_investigation.model.Area
import kotlinx.android.synthetic.main.fragment_create_area_step2.*

/**
 * A placeholder fragment containing a simple view.
 */
class EditAreaActivityFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_edit_area, container, false)

    lateinit private var area: Area

    private val markerBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.marker_edit_mode),
                (32 * pixelsPerDp).toInt(),
                (32 * pixelsPerDp).toInt(),
                false
        )
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)
        area = activity.intent.getParcelableExtra<Area>(AreaListActivity.TAG_AREA)
        // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
        map.map.setOnMapLoadedListener {
            map.map.apply {
                moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds.Builder().apply {
                                    area.outline.forEach { include(it) }
                                }.build(), (8 * pixelsPerDp).toInt()))
                addPolygon(PolygonOptions()
                        .add(*area.outline.toTypedArray())
                        .fillColor(ContextCompat.getColor(activity, R.color.colorAccentLighter))
                        .strokeWidth((1 * pixelsPerDp).toFloat())
                        .strokeColor(ContextCompat.getColor(activity, R.color.colorAccent)))
                area.outline.forEach {
                    addMarker(MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            .position(it)
                            .anchor(0.5F, 0.5F))
                }
                setOnMarkerClickListener { activity.toast("clicked!")
                true
                }
            }
        }
    }
}
