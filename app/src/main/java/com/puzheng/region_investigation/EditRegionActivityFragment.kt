package com.puzheng.region_investigation

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.*
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.POITypeStore
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.fragment_create_region_step2.*
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import java.io.IOException

private enum class MarkerType {
    POI, OUTLINE_VERTEX,
    UNKOWN
}


/**
 * 该fragment会请求ACCESS_FINE_LOCATION权限，所以使用该fragment的activity必须支持在
 * onRequestPermissionsResult中对此进行如下处理
 *
 * <pre>
 * {@code
 * override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
 *     when (requestCode) {
 *     EditRegionActivityFragment.REQUEST_ACCESS_FINE_LOCATION ->
 *          if (grantResults.isNotEmpty()
 *              && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
 *              editRegionActivityFragment.onPermissionGranted(ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION)
 *          }
 *     }
 * }
 * </pre>
 */
class EditRegionActivityFragment : Fragment(), OnPermissionGrantedListener {

    val selectedPOI: POI?
        get() = selectedPOIMarker?.`object` as POI

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

    var editMode: EditMode = EditMode.DEFAULT
        set(value) {
            field = value
            // 设置编辑模式包含两步： 如何展示信息点和边界; 如何设置地图的响应事件
            when (value) {
                EditMode.DEFAULT ->
                    enterDefaultEditMode()
                EditMode.EDIT_OUTLINE ->
                    enterOutlineEditMode()
                EditMode.POI_RELOCATE ->
                    enterPOIRelocateEditMode()
            }
        }

    private fun enterOutlineEditMode() {
        // 编辑边界模式
        hotCopyRegion = listener?.region!!.copy()

        outlineMarkers.forEach {
            it.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerEditModeBitmap))
            it.isDraggable = true
        }
        selectedPOIMarker?.selected = false
        // 为了避免POI和顶点互相干扰，隐藏POI5
        poiMarkers.forEach { it.isVisible = false }

        listener?.onPOIMarkerSelected(null)

        map.map.apply {
            setOnMapLongClickListener {
                latLng ->
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
            setOnMarkerClickListener {
                if (it.selectable) {
                    it.selected = true
                    listener?.onOutlineMarkerSelected(it.position)
                }
                true
            }
            setOnMapClickListener {
                selectedOutlineMarker?.selected = false
                listener?.onOutlineMarkerSelected(null)
            }
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
                    val (scrollX, scrollY) = violationToBoundary(p0!!.screenLocation)
                    map.map.moveCamera(CameraUpdateFactory.scrollBy(scrollX, scrollY))
                }
            })
            resetCamera()
        }
    }

    private fun enterDefaultEditMode() {
        // 默认模式下，只能选择信息点，不能做出任何实际的修改
        hotCopyRegion = listener?.region!!.copy()

        outlineMarkers.forEach {
            it.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerBitmap))
            it.isDraggable = true
        }
        poiMarkers.forEach {
            val poi = it.`object` as POI
            it.isVisible = !hiddenPOITypes.any { poi.poiTypeName == it.name }
        }
        selectedPOIMarker?.selected = false
        listener?.onPOIMarkerSelected(null)

        map.map.apply {
            setOnMapLongClickListener {
                listener?.onMapLongClick()
            }
            setOnMarkerClickListener {
                Logger.v("clicked ${it.type.toString()}")
                if (it.selectable) {
                    it.selected = true
                    listener?.onPOIMarkerSelected(it)
                }
                true
            }
            setOnMapClickListener {
                selectedPOIMarker?.selected = false
                listener?.onPOIMarkerSelected(null)
            }
            setOnMarkerDragListener(null)
        }
    }

    private fun enterPOIRelocateEditMode() {
        outlineMarkers.forEach {
            it.setIcon(BitmapDescriptorFactory.fromBitmap(outlineMarkerBitmap))
            it.isDraggable = true
        }
        poiMarkers.forEach {
            val poi = it.`object` as POI
            it.isVisible = !hiddenPOITypes.any { poi.poiTypeName == it.name }
        }
        map.map.apply {
            setOnMapLongClickListener(null)
            setOnMarkerClickListener(null)
            setOnMapClickListener {
                selectedPOIMarker?.position = it
            }
            setOnMarkerDragListener(null)
        }
    }

    var hiddenPOITypes: Set<POIType> = setOf()
        set(set) {
            field = set
            poiMarkers.forEach {
                val poi = it.`object` as POI
                it.isVisible = !hiddenPOITypes.any { poi.poiTypeName == it.name }
            }
        }
    private val horizontalBoundaryLimit: Int by lazy {
        (40 * pixelsPerDp).toInt()
    }
    private val verticalBoundaryLimit: Int by lazy {
        (40 * pixelsPerDp).toInt()
    }

    companion object {
        val REQUEST_ACCESS_COARSE_LOCATION = uniqueId()

        enum class EditMode {
            DEFAULT, EDIT_OUTLINE, POI_RELOCATE
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_edit_region, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)

        // 不要被SET迷惑，这里实际的意义是GET到地图的onLocationChangeListener
        map.map.setLocationSource(object : LocationSource {
            override fun deactivate() {
                onLocationChangeListener = null
            }


            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                // 这里将获取map默认的OnLocationChangedListener, map不能直接移动中心点，要通过操作这个对象来
                // 实现定位
                onLocationChangeListener = p0
                (activity as AppCompatActivity).assertPermission(Manifest.permission.ACCESS_COARSE_LOCATION,
                        REQUEST_ACCESS_COARSE_LOCATION) successUi {
                    onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_ACCESS_COARSE_LOCATION)
                }
            }
        })
        map.map.uiSettings.isMyLocationButtonEnabled = true

        map.map.setOnMapLoadedListener {
            task {
                poiTypeStore.listSync?.forEach {
                    poiTypeMap[it.name] = it
                }
                Logger.v(poiTypeMap.toString())
                listener?.region!!.poiListSync?.filter {
                    poiTypeMap[it.poiTypeName] != null
                }?.forEach {
                    pois.add(it)
                }
            } successUi {
                // 必须在地图加载完毕之后才可以缩放，否则会产生缩放不正确的情况
                map.map.isMyLocationEnabled = true
                map.map.apply {
                    resetCamera()
                    setupOutline()
                    setupPOIs()
                    editMode = EditMode.DEFAULT
                }
            } failUi {
                it.printStackTrace()
                activity.toast(it.toString())
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

    private val invoked = mutableSetOf<String>()
    private fun once(fn: () -> Unit, key: String): () -> Unit {
        return {
            if (!invoked.contains(key)) {
                invoked.add(key)
                fn()
            }
        }
    }

    override fun onPermissionGranted(permission: String, requestCode: Int) {
        LocateMyselfHelper(activity, onLocationChangeListener!!).locate().always {
            once({
                map.postDelayed({
                    // 注意，由于是delayed操作，activity可能为空
                    if (activity != null) {
                        map.map.resetCamera()
                    }
                }, 1000)
            }, "RESET_CAMERA_AFTER_LOCATE_MYSELF")()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // thanks to http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
    private fun nearestPoint2SegDistance(p: Point, seg: Pair<Point, Point>): Double {
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
        fun onPOIRemoved(poi: POI)
        fun onPOILocationSaved(poi: POI)
        var region: Region
    }

    fun deleteVertex(vertex: LatLng) {
        // 注意，实际的存储中顶点数，一定是边数+1
        if (hotCopyRegion.outline.size <= 4) {
            activity.toast(R.string.outline_needs_be_polygon)
            return
        }
        // 这里有个很tricky的处理逻辑， 如果删除的是出发点，那么首尾顶点都要删除，
        // 而由于第二个点现在成为新的出发点，所以要在最后补上一个新的出发点
        if (hotCopyRegion.outline[0] == vertex) {
            Logger.e("出发点被删除")
            hotCopyRegion.outline = hotCopyRegion.outline.subList(1, hotCopyRegion.outline.size - 1) + hotCopyRegion.outline[1]
        } else {
            hotCopyRegion.outline = hotCopyRegion.outline.filter {
                it != vertex
            }
        }
        map.map.setupOutline()
    }

    fun saveOutline(afterSaving: () -> Unit) {
        if (listener?.region?.outline != hotCopyRegion.outline) {
            ConfirmSaveRegionOutlineDialog(hotCopyRegion, {
                listener?.region = hotCopyRegion
                afterSaving()
            }).show(activity.supportFragmentManager, "")
        } else {
            activity.toast("边界没有发生变化, 不用保存!");
        }
    }

    fun savePOILocation() {
        val poi = selectedPOIMarker!!.`object` as POI
        val latLng = selectedPOIMarker!!.position
        ConfirmSavePOILocationDialog(poi, latLng, {
            selectedPOIMarker!!.`object` = poi.copy(latLng = latLng)
            activity.toast(R.string.poi_location_modified)
            listener?.onPOILocationSaved(poi)
        }).show(activity.supportFragmentManager, "")
    }

    fun restoreOutline() {
        hotCopyRegion = listener?.region!!
        map.map.setupOutline()
    }

    fun restorePOILocation() {
        selectedPOIMarker?.position = (selectedPOIMarker?.`object` as POI).latLng
    }

    private val defaultIconBitmap: Bitmap
        get() = activity.loadBitmap(R.drawable.ic_place_pink_a400_24dp)

    private val defaultActiveIconBitmap: Bitmap
        get() = activity.loadBitmap(R.drawable.ic_place_pink_a100_24dp)


    private fun getIconBitmap(poi: POI): Bitmap? {
        val poiType = poiTypeMap[poi.poiTypeName] ?: return defaultIconBitmap
        val bitmap = try {
            activity.loadBitmap(poiTypeStore.getPOITypeIcon(poiType))
        } catch(e: IOException) {
            null
        } ?: return defaultIconBitmap
        if (!poiTypeIconMap.containsKey(poiType.name)) {
            poiTypeIconMap[poiType.name] = Bitmap.createScaledBitmap(
                    bitmap,
                    (24 * pixelsPerDp).toInt(),
                    (24 * pixelsPerDp).toInt(),
                    false
            )
        }
        return poiTypeIconMap[poiType.name]!!
    }

    private fun getActiveIconBitmap(poi: POI): Bitmap? {
        val poiType = poiTypeMap[poi.poiTypeName] ?: return defaultActiveIconBitmap
        val bitmap = try {
            activity.loadBitmap(poiTypeStore.getPOITypeActiveIcon(poiType))
        } catch (e: IOException) {
            null
        } ?: return defaultActiveIconBitmap
        if (!poiTypeActiveIconMap.containsKey(poiType.name)) {
            poiTypeActiveIconMap[poiType.name] = Bitmap.createScaledBitmap(
                    bitmap,
                    (28 * pixelsPerDp).toInt(),
                    (28 * pixelsPerDp).toInt(),
                    false
            )
        }
        return poiTypeActiveIconMap[poiType.name]!!
    }

    /**
     * @return MarkerOption 如果没有对应的类型信息（可能由该类型信息文件被删除引起）， 返回空
     */
    private fun makeMarkerOption(poi: POI): MarkerOptions? {
        val bitmap = getIconBitmap(poi) ?: return null
        return MarkerOptions().anchor(0.5f, 0.5f).position(poi.latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)).draggable(false)
    }


    fun addPOI(poi: POI): Marker {
        pois.add(poi)
        return map.map.addMarker(makeMarkerOption(poi)!!).apply {
            poiMarkers.add(this)
            `object` = poi
            val isHidden = hiddenPOITypes.any { it.name == poi.poiTypeName }
            if (isHidden) {
                isVisible = false
            } else {
                selected = true
                listener?.onPOIMarkerSelected(this)
            }
        }
    }

    fun removeSelectedPOIMarker() {
        ConfirmRemovePOIDialogFragment({
            val poi = selectedPOIMarker?.`object` as POI
            POIStore.with(activity).remove(poi) and RegionStore.with(activity).touch(poi.regionId) successUi {
                activity.toast(R.string.poi_deleted)
                pois.remove(poi)
                val marker = poiMarkers.find { (it.`object` as POI) == poi }
                marker?.remove()
                poiMarkers.remove(marker)
                listener?.onPOIRemoved(poi)
            }
        }).show(activity.supportFragmentManager, "")
    }

    private fun AMap.resetCamera() {
        moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds.Builder().apply {
                            listener?.region!!.outline.forEach { include(it) }
                            pois.forEach { include(it.latLng) }
                        }.build(), (8 * pixelsPerDp).toInt()))
    }

    // MR extensions
    private val Marker.screenLocation: Point
        get() = map.map.projection.toScreenLocation(position)

    /**
     * marker的类型，注意，当前位置也是一个特殊的Marker，这个marker一定要区分出来，不能被选中
     */
    private val Marker.type: MarkerType
        get() = if (outlineMarkers.contains(this)) MarkerType.OUTLINE_VERTEX
        else if (poiMarkers.contains(this)) MarkerType.POI
        else MarkerType.UNKOWN

    private val Marker.selectable: Boolean
        get() = when (type) {
            MarkerType.OUTLINE_VERTEX ->
                editMode == EditMode.EDIT_OUTLINE
            MarkerType.POI ->
                editMode == EditMode.DEFAULT
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
                    .icon(BitmapDescriptorFactory.fromBitmap(
                            if (editMode == EditMode.EDIT_OUTLINE) outlineMarkerEditModeBitmap else outlineMarkerBitmap))
                    .position(latLng)
                    .anchor(0.5F, 0.5F).draggable(editMode == EditMode.EDIT_OUTLINE))


    private fun AMap.setupOutline() {
        val region = if (editMode == EditMode.EDIT_OUTLINE) hotCopyRegion else listener?.region!!
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
