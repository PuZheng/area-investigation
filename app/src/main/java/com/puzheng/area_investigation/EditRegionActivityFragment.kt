package com.puzheng.area_investigation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.*
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Region
import com.puzheng.area_investigation.model.POI
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.RegionStore
import com.puzheng.area_investigation.store.POITypeStore
import kotlinx.android.synthetic.main.content_edit_area.*
import kotlinx.android.synthetic.main.fragment_create_area_step2.*
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi

private enum class MarkerType {
    POI, OUTLINE_VERTEX
}

/**
 * 该fragment会请求ACCESS_FINE_LOCATION权限，所以使用该fragment的activity必须支持在
 * onRequestPermissionsResult中对此进行如下处理
 *
 * <pre>
 * {@code
 * override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
 *     when (requestCode) {
 *     EditAreaActivityFragment.REQUEST_ACCESS_FINE_LOCATION ->
 *          if (grantResults.isNotEmpty()
 *              && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
 *              editAreaActivityFragment.locate()
 *          }
 *     }
 * }
 * </pre>
 */
class EditRegionActivityFragment : Fragment(), OnPermissionGrantedListener {


    lateinit private var originRegion: Region
    lateinit private var hotCopyRegion: Region // 用于保存编辑状态下的区域信息
    private val outlineMarkerBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.marker),
                (8 * pixelsPerDp).toInt(),
                (8 * pixelsPerDp).toInt(),
                false
        )
    }
    private val outlineMarkerEditModeBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.marker_edit_mode),
                (32 * pixelsPerDp).toInt(),
                (32 * pixelsPerDp).toInt(),
                false
        )
    }
    private val touchingMarkerBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.touching_marker),
                (64 * pixelsPerDp).toInt(),
                (64 * pixelsPerDp).toInt(),
                false
        )
    }

    private val selectedOutlineMarkerBitmap: Bitmap by lazy {
        Bitmap.createScaledBitmap(
                activity.loadBitmap(R.drawable.selected_marker),
                (32 * pixelsPerDp).toInt(),
                (32 * pixelsPerDp).toInt(),
                false
        )
    }
    private var outlineMarkers = mutableSetOf<Marker>()
    private var outlineSegs = mutableSetOf<Polyline>()
    private var selectedOutlineMarker: Marker? = null
    private var outlinePolygon: Polygon? = null
    private val POLYLINE_CLOSE_LIMIT: Double by lazy { 8 * pixelsPerDp }
    private val poiTypeIconMap: MutableMap<String, Bitmap> = mutableMapOf()
    private val poiTypeActiveIconMap: MutableMap<String, Bitmap> = mutableMapOf()
    private var selectedPOIMarker: Marker? = null
    private var poiMarkers = mutableSetOf<Marker>()
    private var pois = mutableListOf<POI>()
    private var poiTypeMap = mutableMapOf<String, POIType>()
    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(activity)
    }
    private var onLocationChangeListener: LocationSource.OnLocationChangedListener? = null

    var editOutlineMode: Boolean = false
        set(b) {
            field = b
            if (b) {
                outlineMarkers.forEach {
                    it.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerEditModeBitmap))
                    it.isDraggable = true
                }
                hotCopyRegion = originRegion.copy()
                selectedPOIMarker?.selected = false
                listener?.onPOIMarkerSelected(null)
                // 为了避免POI和顶点互相干扰，隐藏POI5
                poiMarkers.forEach { it.isVisible = false }
            } else {
                // 恢复为初始数据
                hotCopyRegion = originRegion.copy()
                map.map.setupOutline()
                poiMarkers.forEach { it.isVisible = true }
            }

        }

    var hiddenPOITypes: Set<POIType> = setOf()
        set(set) {
            field = set
            map.map.setupPOIs()
        }


    companion object {
        val REQUEST_ACCESS_FINE_LOCATION = 100
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_edit_area, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)
        originRegion = activity.intent.getParcelableExtra<Region>(RegionListActivity.TAG_AREA)

        // 不要被SET迷惑，这里实际的意义是GET到地图的onLocationChangeListener
        map.map.setLocationSource(object : LocationSource {
            override fun deactivate() {
                onLocationChangeListener = null
            }


            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                // 这里将获取map默认的OnLocationChangedListener, map不能直接移动中心点，要通过操作这个对象来
                // 实现定位
                onLocationChangeListener = p0
                activity.assertPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION) successUi {
                    onPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION)
                }
            }
        })
        map.map.uiSettings.isMyLocationButtonEnabled = true

        Logger.v(originRegion.toString())
        RegionStore.with(activity).getPOIList(originRegion) successUi  {
            it?.forEach {
                pois.add(it)
            }
        } failUi {
            activity.toast(it.toString())
        }
        poiTypeStore.list.successUi {
            it?.forEach {
                poiTypeMap[it.uuid] = it
            }
        }
        // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
        map.map.setOnMapLoadedListener {
            map.map.isMyLocationEnabled = true
            map.map.apply {
                resetCamera()
                setupOutline()
                setupPOIs()
                setOnMapLongClickListener {
                    latLng ->
                    listener?.onMapLongClick()
                    if (editOutlineMode) {
                        val polyline = findPolylineCloseTo(latLng)
                        if (polyline != null) {
                            val index = hotCopyRegion.outline.indexOf(polyline.points[0])
                            hotCopyRegion.outline = hotCopyRegion.outline.toMutableList().apply { add(index + 1, latLng) }
                            setupOutline()
                            // select the newly created marker
                            outlineMarkers.forEach {
                                if (it.position == latLng) {
                                    it.selected = true
                                    listener?.onOutlineMarkerSelected(latLng)
                                }
                            }
                        }
                    }
                }
                setOnMarkerClickListener {
                    Logger.v("clicked ${it.type.toString()}")
                    if (it.selectable) {
                        it.selected = true
                        if (editOutlineMode) {
                            listener?.onOutlineMarkerSelected(it.position)
                        } else {
                            listener?.onPOIMarkerSelected(it)
                        }
                    }
                    true
                }
                setOnMapClickListener {
                    selectedOutlineMarker?.selected = false
                    selectedPOIMarker?.selected = false
                    if (editOutlineMode) {
                        listener?.onOutlineMarkerSelected(null)
                    } else {
                        listener?.onPOIMarkerSelected(null)
                    }
                }

                // 只有轮廓顶点可以被拖动
                setOnMarkerDragListener(object : AMap.OnMarkerDragListener {

                    override fun onMarkerDragEnd(p0: Marker?) {
                        p0?.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerEditModeBitmap))
                        hotCopyRegion.outline = outlineMarkers.map { it.position }
                        setupOutline()
                    }

                    override fun onMarkerDragStart(p0: Marker?) {
                        p0?.setIcon(BitmapDescriptorFactory.fromBitmap(touchingMarkerBitmap))
                    }

                    override fun onMarkerDrag(p0: Marker?) {

                    }
                })
            }


        }
    }

    override fun onPermissionGranted(permission: String, requestCode: Int) {
        LocateMyselfHelper(activity, onLocationChangeListener!!).locate().always {
            map.postDelayed({
                // 注意，由于是delayed操作，activity可能为空
                if (activity != null) {
                    map.map.resetCamera()
                }
            }, 1000)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // thanks to http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
    private fun nearestPoint2SegDistance(p: Point, seg: Pair<Point, Point>): Double {
        Logger.v(p.toString() + "-" + seg.toString())
        fun squaredDistance(p0: Point, p1: Point) = Math.pow((p0.x - p1.x).toDouble(), 2.0) +
                Math.pow((p0.y - p1.y).toDouble(), 2.0)

        fun distance(p0: Point, p1: Point) = Math.sqrt(squaredDistance(p0, p1))

        val l2 = squaredDistance(seg.first, seg.second)  // i.e. |w-v|^2 -  avoid a sqrt
        if (l2 == 0.0) return distance(p, seg.first)   // v == w case
        // Consider the line extending the segment, parameterized as v + t (w - v).
        // We find projection of point p onto the line.
        // It falls where t = [(p-v) . (w-v)] / |w-v|^2
        // We clamp t from [0,1] to handle points outside the segment vw.
        var t = ((p.x - seg.first.x) * (seg.second.x - seg.first.x) + (p.y - seg.first.y) * (seg.second.y - seg.first.y)) / l2
        t = Math.max(0.0, Math.min(1.0, t))
        return distance(p, Point(seg.first.x + (t * (seg.second.x - seg.first.x)).toInt(),
                seg.first.y + (t * (seg.second.y - seg.first.y)).toInt()))
    }


    private fun findPolylineCloseTo(latLng: LatLng) = outlineSegs.map {
        nearestPoint2SegDistance(latLng.screenLocation, Pair(it.points[0].screenLocation, it.points[1].screenLocation)) to it
    }.filter {
        Logger.v(it.toString())
        it.first < POLYLINE_CLOSE_LIMIT
    }.minBy {
        it.first
    }?.second

    private var listener: OnFragmentInteractionListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    interface OnFragmentInteractionListener {
        fun onMapLongClick()
        fun onOutlineMarkerSelected(position: LatLng?)
        fun onPOIMarkerSelected(marker: Marker?)
    }

    fun deleteVertex(vertex: LatLng) {
        if (!editOutlineMode) {
            return
        }
        if (hotCopyRegion.outline.size <= 3) {
            activity.toast(R.string.outline_needs_3_points)
            return
        }
        hotCopyRegion.outline = hotCopyRegion.outline.filter {
            it != vertex
        }
        map.map.setupOutline()
    }

    fun saveOutline(afterSaving: () -> Unit) {
        ConfirmSaveRegionOutlineDialog(hotCopyRegion, {
            originRegion = hotCopyRegion
            afterSaving()
        }).show(activity.supportFragmentManager, "")
    }

    private fun getIconBitmap(poi: POI): Bitmap? {
        val poiType = poiTypeMap[poi.poiTypeUUID] ?: return null
        if (!poiTypeIconMap.containsKey(poiType.uuid)) {
            poiTypeIconMap[poiType.uuid] = Bitmap.createScaledBitmap(
                    activity.loadBitmap(poiTypeStore.getPOITypeIcon(poiType)),
                    (24 * pixelsPerDp).toInt(),
                    (24 * pixelsPerDp).toInt(),
                    false
            )
        }
        return poiTypeIconMap[poiType.uuid]!!
    }

    private fun getActiveIconBitmap(poi: POI): Bitmap? {
        val poiType = poiTypeMap[poi.poiTypeUUID] ?: return null
        if (!poiTypeActiveIconMap.containsKey(poiType.uuid)) {
            poiTypeActiveIconMap[poiType.uuid] = Bitmap.createScaledBitmap(
                    activity.loadBitmap(poiTypeStore.getPOITypeActiveIcon(poiType)),
                    (28 * pixelsPerDp).toInt(),
                    (28 * pixelsPerDp).toInt(),
                    false
            )
        }
        return poiTypeActiveIconMap[poiType.uuid]!!
    }

    /**
     * @return MarkerOption 如果没有对应的类型信息（可能由该类型信息文件被删除引起）， 返回空
     */
    private fun makeMarkerOption(poi: POI): MarkerOptions? {
        val bitmap = getIconBitmap(poi) ?: return null
        return MarkerOptions().anchor(0.5f, 0.5f).position(poi.latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)).draggable(false)
    }


    fun addPOI(poi: POI) {
        map.map.addMarker(makeMarkerOption(poi)!!).`object` = poi
        pois.add(poi)
        map.map.resetCamera()
    }

    fun removePOI(poi: POI) {
        pois.remove(poi)
        map.map.setupPOIs()
        map.map.resetCamera()
    }

    private fun AMap.resetCamera() {
        moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds.Builder().apply {
                            originRegion.outline.forEach { include(it) }
                            pois.forEach { include(it.latLng) }
                        }.build(), (8 * pixelsPerDp).toInt()))
    }

    // MR extensions
    private val Marker.type: MarkerType
        get() = if (outlineMarkers.contains(this)) MarkerType.OUTLINE_VERTEX else MarkerType.POI

    private val Marker.selectable: Boolean
        get() = when (type) {
            MarkerType.OUTLINE_VERTEX ->
                editOutlineMode
            MarkerType.POI ->
                !editOutlineMode
            else -> false
        }

    private val LatLng.screenLocation: Point
        get() = map.map.projection.toScreenLocation(this)


    private var Marker.selected: Boolean
        get() = when (this.type) {
            MarkerType.OUTLINE_VERTEX ->
                selectedOutlineMarker == this
            MarkerType.POI ->
                selectedPOIMarker == this
            else ->
                false
        }
        set(b: Boolean) {
            when (this.type) {
                MarkerType.OUTLINE_VERTEX -> {
                    selectedOutlineMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerEditModeBitmap))
                    if (b) {
                        selectedOutlineMarker = this
                        setIcon(BitmapDescriptorFactory.fromBitmap(selectedOutlineMarkerBitmap))
                    }
                }
                MarkerType.POI -> {
                    selectedPOIMarker?.apply {
                        setIcon(BitmapDescriptorFactory.fromBitmap(getIconBitmap(`object` as POI)))
                    }
                    if (b) {
                        selectedPOIMarker = this
                        setIcon(BitmapDescriptorFactory.fromBitmap(getActiveIconBitmap(`object` as POI)))
                    }
                }
                else -> {
                }
            }
        }

    private fun AMap.setupPOIs() {
        poiMarkers.forEach { it.remove() }
        poiMarkers.clear()
        pois.forEach {
            poi ->
            val markerOption = makeMarkerOption(poi)
            if (markerOption != null) {
                poiMarkers.add(addMarker(markerOption).apply {
                    `object` = poi
                    isVisible = !hiddenPOITypes.any { poi.poiTypeUUID == it.uuid }
                })
            }
        }
    }


    private fun AMap.addPolygon(outline: Array<LatLng>) =
            addPolygon(PolygonOptions()
                    .add(*outline)
                    .fillColor(ContextCompat.getColor(activity, R.color.colorOutlinePolygon))
                    .strokeColor(ContextCompat.getColor(activity, android.R.color.transparent)))

    private fun AMap.addOutlineMarker(latLng: LatLng) =
            addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(if (editOutlineMode) outlineMarkerEditModeBitmap else outlineMarkerBitmap))
                    .position(latLng)
                    .anchor(0.5F, 0.5F).draggable(editOutlineMode))


    private fun AMap.setupOutline() {
        val region = if (editOutlineMode) hotCopyRegion else originRegion
        outlinePolygon?.remove()
        outlinePolygon = addPolygon(region.outline.toTypedArray())
        outlineMarkers.apply {
            forEach {
                it.remove()
            }
            clear()
            region.outline.forEach {
                this@apply.add(this@setupOutline.addOutlineMarker(it))
            }
        }
        selectedOutlineMarker = null
        listener?.onOutlineMarkerSelected(null)
        outlineSegs.apply {
            forEach { it.remove() }
            clear()
            region.outline.withIndex().forEach {
                val (idx, latLng) = it
                this@apply.add(
                        addPolyline(PolylineOptions().add(region.outline[(idx - 1 + region.outline.size) % region.outline.size],
                                latLng).width((2 * pixelsPerDp).toFloat())
                                .color(ContextCompat.getColor(activity, R.color.colorOutlinePolyline)))
                )
            }
        }
    }

}
