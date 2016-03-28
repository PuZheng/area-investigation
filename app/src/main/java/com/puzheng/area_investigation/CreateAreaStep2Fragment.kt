package com.puzheng.area_investigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.MotionEventCompat
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.LocationSource
import com.amap.api.maps2d.model.*
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.fragment_create_area_step2.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CreateAreaStep2Fragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CreateAreaStep2Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateAreaStep2Fragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    private val closeToLimit: Int by lazy {
        (32 * pixelsPerDp).toInt()
    }

    private val horizontalBoundaryLimit: Int by lazy {
        (40 * pixelsPerDp).toInt()
    }

    private val verticalBoundaryLimit: Int by lazy {
        (40 * pixelsPerDp).toInt()
    }

    // 地图清除所有覆盖物时，会回收所有关联的Bitmap
    private val touchingMarkerBitmap: Bitmap? = null
        get() = if (field == null || field.isRecycled) activity.loadBitmap(R.drawable.touching_marker) else field

    private val closingMarkerBitmap: Bitmap? = null
        get() = if (field == null || field.isRecycled) activity.loadBitmap(R.drawable.closing_marker) else field

    private val markerBitmap: Bitmap? = null
        get() = if (field == null || field.isRecycled) activity.loadBitmap(R.drawable.marker) else field




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?) = inflater!!.inflate(R.layout.fragment_create_area_step2, container, false)


    private var onLocationChangeListener: LocationSource.OnLocationChangedListener? = null

    private val isAreaClosed: Boolean
        get() = markers.size > 2 && markers.first().screenLocation == markers.last().screenLocation

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map.onCreate(savedInstanceState)
        map.map.setLocationSource(object : LocationSource {
            override fun deactivate() {
                onLocationChangeListener = null
            }

            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                // 这里将获取map默认的OnLocationChangedListener, map不能直接移动中心点，要通过操作这个对象来
                // 实现定位
                onLocationChangeListener = p0
            }
        })
        map.map.isMyLocationEnabled = true
        map.map.moveCamera(CameraUpdateFactory.zoomTo(INIT_ZOOM_LEVEL))
        map.map.setOnMapLongClickListener {
            listener?.onMapLongClick(this, it)
        }
        var lastScreenLocation: Point? = null
        map.map.setOnMapTouchListener {
            if (!map.map.uiSettings.isScrollGesturesEnabled) {
                when (MotionEventCompat.getActionMasked(it)) {
                    MotionEvent.ACTION_UP -> {
                        activeMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                        activePolyline.color = ContextCompat.getColor(activity, R.color.colorAccent)
                        startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                        if (isAreaClosed) {
                            listener?.onDrawDone(markers.map {
                                it.position
                            })
                            stopDraw()
                        }
                    }
                    MotionEvent.ACTION_DOWN -> {
                        addMarker(it.latlng)
                        lastScreenLocation = it.screenLocation
                    }
                    MotionEvent.ACTION_MOVE -> {
                        activeMarker?.position = it.latlng
                        // 一定要构成多边形
                        if (markers.size > 2) {
                            if (isCloseToStartMarker(it.screenLocation, lastScreenLocation)) {
                                startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(closingMarkerBitmap))
                                // 自动吸附到出发点
                                activeMarker?.position = startMarker!!.position
                            } else {
                                startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            }
                        }
                        val (scrollX, scrollY) = violationToBoundary(it.screenLocation)
                        Logger.v("$scrollX $scrollY")
                        map.map.moveCamera(CameraUpdateFactory.scrollBy(scrollX, scrollY))
                        activePolyline.points = listOf(activeMarker?.position, markers[markers.lastIndex - 1].position)
                        lastScreenLocation = it.screenLocation
                    }
                }
            }
        }

        val permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // TODO Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERSISSION_TO_ACCESS_FINE_LOCATION)
            }
        }
    }



    private fun violationToBoundary(screenLocation: Point): FloatArray {
        val scrollX = if (screenLocation.x < horizontalBoundaryLimit) {
            screenLocation.x - horizontalBoundaryLimit.toFloat()
        } else if (screenLocation.x + horizontalBoundaryLimit > map.width) {
            screenLocation.x + horizontalBoundaryLimit.toFloat() - map.width
        } else {
            0.0F
        }
        val scrollY = if (screenLocation.y < verticalBoundaryLimit) {
            screenLocation.y - verticalBoundaryLimit.toFloat()
        } else if (screenLocation.y + verticalBoundaryLimit > map.height) {
            screenLocation.y + verticalBoundaryLimit.toFloat() - map.height
        } else {
            0.0F
        }
        return floatArrayOf(scrollX, scrollY)
    }

    private fun isCloseToStartMarker(screenLocation: Point, lastScreenLocation: Point?): Boolean {
        if (startMarker == null) {
            return false
        }
        val isApproaching = lastScreenLocation == null || (
                lastScreenLocation!!.distanceTo(startMarker!!.screenLocation) >
                        screenLocation.distanceTo(startMarker!!.screenLocation))
        return isApproaching && startMarker!!.screenLocation.distanceTo(screenLocation) < closeToLimit
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERSISSION_TO_ACCESS_FINE_LOCATION ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocation()
                }
        }
    }

    private fun startLocation() {
        //初始化定位
        val locationClient = AMapLocationClient(activity)
        //设置定位回调监听
        locationClient.setLocationListener({
            if (it?.errorCode == 0) {
                Logger.e(it.toStr())
                onLocationChangeListener?.onLocationChanged(it)
            } else {
                Logger.e("定位失败, ${it.errorCode}: ${it.errorInfo}");
            }
        })
        //初始化定位参数
        val locationOption = AMapLocationClientOption()
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;
        locationOption.isOnceLocation = true // 只需要定位一次
        locationClient.setLocationOption(locationOption);
        //启动定位
        locationClient.startLocation();
    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onMapLongClick(fragment: CreateAreaStep2Fragment, lnglat: LatLng)

        fun onDrawDone(latLngList: List<LatLng>)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment CreateAreaStep2Fragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance() = CreateAreaStep2Fragment()

        private val PERSISSION_TO_ACCESS_FINE_LOCATION: Int = 100
        private val INIT_ZOOM_LEVEL: Float = 16.0F

    }

    fun startDraw() {
        map.map.uiSettings.isScrollGesturesEnabled = false
        map.map.uiSettings.isZoomGesturesEnabled = false
    }

    fun stopDraw() {
        map.map.uiSettings.isScrollGesturesEnabled = true
        map.map.uiSettings.isZoomGesturesEnabled = true
        markers.clear()
        map.map.clear()
    }

    private var activeMarker: Marker? = null
        get() = markers.last()

    private var startMarker: Marker? = null
        get() = markers.first()

    private val markers = mutableListOf<Marker>()

    lateinit private var activePolyline: Polyline

    fun addMarker(latlng: LatLng) {
        markers.add(map.map.addMarker(
                MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(touchingMarkerBitmap))
                        .position(latlng).anchor(0.5F, 0.5F)))
        if (markers.size > 1) {
            activePolyline = map.map.addPolyline(
                    PolylineOptions().add(activeMarker?.position, markers[markers.lastIndex - 1].position)
                            .width(4.0F).color(ContextCompat.getColor(activity, R.color.colorAccentLighter)))
        } else {
            activeMarker?.zIndex = 1000.0F // 出发点要在所有点的上面
        }
    }

    // MR extensions
    private val Marker.screenLocation: Point
        get() = map.map.projection.toScreenLocation(position)

    private val MotionEvent.screenLocation: Point
        get() = Point(x.toInt(), y.toInt())

    private val MotionEvent.latlng: LatLng
        get() = map.map.projection.fromScreenLocation(screenLocation)

    private fun Point.distanceTo(point: Point) = Math.sqrt(
            (Math.pow((point.x - x).toDouble(), 2.0) + Math.pow((point.y - y).toDouble(), 2.0)))
}

