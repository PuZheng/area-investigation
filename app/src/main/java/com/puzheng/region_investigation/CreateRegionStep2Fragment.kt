package com.puzheng.region_investigation

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.MotionEventCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.*
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.fragment_create_region_step2.*
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CreateRegionStep2Fragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CreateRegionStep2Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateRegionStep2Fragment : Fragment(), OnPermissionGrantedListener {

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

    // 地图清除所有覆盖物时，会回收所有关联的Bitmap, 所以要判断是否Recycled
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
                              savedInstanceState: Bundle?) = inflater!!.inflate(R.layout.fragment_create_region_step2, container, false)


    private var onLocationChangeListener: LocationSource.OnLocationChangedListener? = null

    private val isRegionClosed: Boolean
        get() = markers.size > 2 && markers.first().screenLocation == markers.last().screenLocation

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map.onCreate(savedInstanceState)
        map.map.apply {
            // 不要被SET迷惑，这里实际的意义是GET
            setLocationSource(object : LocationSource {
                override fun deactivate() {
                    onLocationChangeListener = null
                }

                override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                    // 这里将获取map默认的OnLocationChangedListener, map不能直接移动中心点，要通过操作这个对象来
                    // 实现定位
                    onLocationChangeListener = p0
                }
            })
            isMyLocationEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            moveCamera(CameraUpdateFactory.zoomTo(INIT_ZOOM_LEVEL))
            setOnMapLongClickListener {
                listener?.onMapLongClick(this@CreateRegionStep2Fragment, it)
            }
            var lastScreenLocation: Point? = null
            setOnMapTouchListener {
                if (!uiSettings.isScrollGesturesEnabled) {
                    when (MotionEventCompat.getActionMasked(it)) {
                        MotionEvent.ACTION_UP -> {
                            activeMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            activePolyline.color = ContextCompat.getColor(activity, R.color.colorAccent)
                            startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            if (isRegionClosed) {
                                // 注意，去掉最后一个点， 因为和第一个点重合
                                listener?.onDrawDone(markers.map {
                                    it.position
                                }.subList(1, markers.size))
                                stopDraw()
                            }
                        }
                        MotionEvent.ACTION_DOWN -> {
                            addMarker(it.latLng)
                            lastScreenLocation = it.screenLocation
                        }
                        MotionEvent.ACTION_MOVE -> {
                            activeMarker?.position = it.latLng
                            // 一定要构成多边形, 注意，这里为什么一定要是3,而不是2？因为我们要求
                            // 最后一个点和第一个点重合， 也就是说如果是三角形，实际上有4个marker
                            if (markers.size > 3) {
                                if (isApproachingToStartMarker(it.screenLocation, lastScreenLocation)) {
                                    startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(closingMarkerBitmap))
                                    // 自动吸附到出发点
                                    activeMarker?.position = startMarker!!.position
                                } else {
                                    startMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                }
                            }
                            val (scrollX, scrollY) = violationToBoundary(it.screenLocation)
                            moveCamera(CameraUpdateFactory.scrollBy(scrollX, scrollY))
                            activePolyline.points = listOf(activeMarker?.position, markers[markers.lastIndex - 1].position)
                            lastScreenLocation = it.screenLocation
                        }
                    }
                }
            }
        }
        (activity as AppCompatActivity).assertPermission(Manifest.permission.ACCESS_COARSE_LOCATION,
                REQUEST_ACCESS_COARSE_LOCATION).successUi {
            onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_ACCESS_COARSE_LOCATION)
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

    private fun isApproachingToStartMarker(screenLocation: Point, lastScreenLocation: Point?): Boolean {
        if (startMarker == null) {
            return false
        }
        val isApproaching = lastScreenLocation == null || (
                lastScreenLocation.distanceTo(startMarker!!.screenLocation) >
                        screenLocation.distanceTo(startMarker!!.screenLocation))

        return if (isApproaching) startMarker!!.screenLocation.distanceTo(screenLocation) < closeToLimit else
            startMarker!!.screenLocation.distanceTo(screenLocation) < 8 * pixelsPerDp
    }


    override fun onPermissionGranted(permission: String, requestCode: Int) {
        LocateMyselfHelper(activity, onLocationChangeListener!!).let {
            helper ->
            helper.locate().failUi {
                // http://stackoverflow.com/questions/17983865/making-a-location-object-in-android-with-latitude-and-longitude-values
                val lastLocation = helper.lastLocation
                if (lastLocation != null) {
                    onLocationChangeListener?.onLocationChanged(AMapLocation(Location("").apply {
                        latitude = lastLocation.latitude
                        longitude = lastLocation.longitude
                    }))
                } else {
                    object: AppCompatDialogFragment() {
                        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                            var ret = AlertDialog.Builder(context)
                                    .setMessage("无法获取您当前位置，可能您没有打开GPS或无线网络!").setPositiveButton("知道了", null).create()
                            // 重新开始不停定位, 直到定位成功
                            val locationClient = AMapLocationClient(context)
                            locationClient.setLocationListener({
                                if (it?.errorCode == 0) {
                                    Logger.e(it.toStr())
                                    onLocationChangeListener?.onLocationChanged(it)
                                    locationClient.stopLocation()
                                } else {
                                    Logger.e("定位失败, ${it.errorCode}: ${it.errorInfo}");
                                }
                            })
                            //初始化定位参数
                            val locationOption = AMapLocationClientOption()
                            //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
                            locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;
                            locationClient.setLocationOption(locationOption)
                            //启动定位
                            locationClient.startLocation()
                            return ret;
                        }
                    }.show(activity.supportFragmentManager, "")
                }
            }
        }
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
        fun onMapLongClick(fragment: CreateRegionStep2Fragment, lnglat: LatLng)

        fun onDrawDone(latLngList: List<LatLng>)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment CreateRegionStep2Fragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance() = CreateRegionStep2Fragment()

        val REQUEST_ACCESS_COARSE_LOCATION: Int = 100
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

    fun addMarker(latLng: LatLng) {
        markers.add(map.map.addMarker(
                MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(touchingMarkerBitmap))
                        .position(latLng).anchor(0.5F, 0.5F)))
        if (markers.size > 1) {
            activePolyline = map.map.addPolyline(
                    PolylineOptions().add(activeMarker?.position, markers[markers.lastIndex - 1].position)
                            .width(pixelsPerDp.toFloat()).color(ContextCompat.getColor(activity, R.color.colorAccentLighter)))
        } else {
            activeMarker?.zIndex = 1000.0F // 出发点要在所有点的上面
        }
    }

    // MR extensions
    private val Marker.screenLocation: Point
        get() = map.map.projection.toScreenLocation(position)

    private val MotionEvent.screenLocation: Point
        get() = Point(x.toInt(), y.toInt())

    private val MotionEvent.latLng: LatLng
        get() = map.map.projection.fromScreenLocation(screenLocation)

    private fun Point.distanceTo(point: Point) = Math.sqrt(
            (Math.pow((point.x - x).toDouble(), 2.0) + Math.pow((point.y - y).toDouble(), 2.0)))
}

