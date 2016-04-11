package com.puzheng.area_investigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ActionMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.Toast
import com.amap.api.location.AMapLocation
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Region
import com.puzheng.area_investigation.model.POI
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.RegionStore
import com.puzheng.area_investigation.store.POIStore
import com.puzheng.area_investigation.store.POITypeStore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit_area.*
import kotlinx.android.synthetic.main.app_bar_edit_area_name.*
import kotlinx.android.synthetic.main.content_edit_area.*
import kotlinx.android.synthetic.main.fragment_edit_area.*
import kotlinx.android.synthetic.main.poi_bottom_sheet.*
import nl.komponents.kovenant.ui.successUi
import java.util.*

private val REQUEST_WRITE_EXTERNAL_STORAGE = 100
private val REQUEST_ACCESS_FINE_LOCATION: Int = 101

class EditRegionActivity : AppCompatActivity(), EditRegionActivityFragment.OnFragmentInteractionListener,
POIFilterDialogFragment.OnFragmentInteractionListener {

    val fragmentEditRegion: EditRegionActivityFragment by lazy {
        findFragmentById<EditRegionActivityFragment>(R.id.fragment_edit_area)!!
    }
    override fun onFilterPOI(hiddenPOITypes: Set<POIType>) {
        fragmentEditRegion.hiddenPOITypes = hiddenPOITypes
        invalidateOptionsMenu()
    }

    private val bottomSheetBehavior: BottomSheetBehavior<out View> by lazy {
        BottomSheetBehavior.from(design_bottom_sheet)
    }

    override fun onPOIMarkerSelected(marker: Marker?) {
        if (marker == null) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            fab?.visibility = View.VISIBLE
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            design_bottom_sheet.findView<ImageButton>(R.id.trash).setOnClickListener {
                ConfirmRemovePOIDialogFragment({
                    val poi = marker.`object` as POI
                    POIStore.with(this).remove(poi).successUi {
                        toast(R.string.poi_deleted)
                        fragmentEditRegion.removePOI(poi)
                    }
                }).show(supportFragmentManager, "")
            }
            design_bottom_sheet.findView<ImageButton>(R.id.edit).setOnClickListener {
                toast("尚未实现")
            }
            design_bottom_sheet.findView<ImageButton>(R.id.relocate).setOnClickListener {
                toast("尚未实现")
            }
            fab?.visibility = View.GONE
        }
    }

    private var selectedVertex: LatLng? = null
    private var dataChanged = false

    override fun onOutlineMarkerSelected(position: LatLng?) {
        selectedVertex = position
        editOutlineActionMode?.menu?.findItem(R.id.action_delete)?.isVisible = selectedVertex != null
    }

    private var editOutlineActionMode: ActionMode? = null

    override fun onMapLongClick() {
        if (editOutlineActionMode != null) {
            return
        }

        editOutlineActionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = when (item?.itemId) {
                R.id.action_delete -> {
                    if (selectedVertex != null) {
                        fragmentEditRegion.deleteVertex(selectedVertex!!)
                    }
                    true
                }
                R.id.action_submit -> {
                    (fragment_edit_area as EditRegionActivityFragment).saveOutline({
                        editOutlineActionMode?.finish()
                        // 注意， 一定要告诉Picasso清除图片缓存
                        Picasso.with(this@EditRegionActivity).invalidate(RegionStore.with(this@EditRegionActivity).getCoverImageFile(region))
                    })
                    dataChanged = true
                    true
                }
                else -> false

            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.context_menu_edit_area_outline, menu);
                fab?.hide()
                fragmentEditRegion.editOutlineMode = true
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                editOutlineActionMode = null
                fragmentEditRegion.editOutlineMode = false
                fab?.show()
            }
        })

    }

    lateinit private var region: Region

    private val permissionRequestHandlerMap: MutableMap<String, () -> Unit> = mutableMapOf()

    private fun fetchPOITypes(after: (List<POIType>) -> Unit) {
        POITypeStore.with(this).list successUi {
            if (it != null && it.isNotEmpty()) {
                after(it)
            } else {
                val store = POITypeStore.with(this@EditRegionActivity)
                this@EditRegionActivity.toast(resources.getString(R.string.no_poi_type_meta_info, store.dir.absolutePath),
                        Toast.LENGTH_LONG)
                permissionRequestHandlerMap[Manifest.permission.WRITE_EXTERNAL_STORAGE] = {
                    mkPOITypeDir()
                }
                assertPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_WRITE_EXTERNAL_STORAGE).successUi {
                    permissionRequestHandlerMap[Manifest.permission.WRITE_EXTERNAL_STORAGE]?.invoke()
                }
            }
        }
        // TODO it should be polite to show a progressbar

    }

    private fun mkPOITypeDir() {
        POITypeStore.with(this).dir.apply {
            Logger.v("poi type dir is $absolutePath")
            if (mkdirs()) {
                Logger.e("can't make directory $absolutePath")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_area)
        Logger.init("EditAreaActivity")
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fab.setOnClickListener {
            fetchPOITypes {
                POITypeChooseDialog(it, { addPOI(it) })
                        .show(supportFragmentManager, "")
            }

        }
        region = intent.getParcelableExtra<Region>(RegionListActivity.TAG_AREA)
        updateContent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_area, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_filter)?.icon = ContextCompat.getDrawable(
                this,
                if (fragmentEditRegion.hiddenPOITypes.isNotEmpty()) {
                    R.drawable.vector_drawable_filter_activated
                } else {
                    R.drawable.vector_drawable_filter
                }
        )
        return super.onPrepareOptionsMenu(menu)
    }

    private var editNameActionMode: ActionMode? = null

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.action_edit_name -> {
            editNameActionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    mode?.customView = layoutInflater.inflate(R.layout.app_bar_edit_area_name, null, false)
                    area_name.apply {
                        setText(region.name)
                        requestFocus()
                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(currentFocus, 0)
                        setSelection(region.name.length)
                    }
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    when (item?.itemId) {
                        R.id.action_ok -> {
                            editNameActionMode!!.finish()
                            region.name = area_name.text.toString()
                            updateContent()
                            RegionStore.with(this@EditRegionActivity).updateAreaName(region.id!!, area_name.text.toString()) successUi {
                                toast(R.string.edit_area_name_success)
                                dataChanged = true
                            }
                        }
                    }
                    return true
                }


                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    mode?.menuInflater?.inflate(R.menu.context_menu_edit_area_name, menu)
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                }
            })
            true
        }
        R.id.action_show_stat -> {
            RegionStatDialogFragment(region).show(supportFragmentManager, "")
            true
        }
        R.id.action_filter -> {
            POIFilterDialogFragment(region,
                    fragmentEditRegion.hiddenPOITypes).show(supportFragmentManager, "")
            true
        }
        else ->
            super.onOptionsItemSelected(item)
    }

    private fun updateContent() {
        supportActionBar?.title = region.name
    }

    companion object {
        val TAG_AREA_ID = "AREA_ID"

    }

    fun addPOI(poiType: POIType) {
        permissionRequestHandlerMap[Manifest.permission.ACCESS_FINE_LOCATION] = {
            getLocation(AMapLocation(Location("").apply {
                latitude = center.latitude
                longitude = center.longitude
            })).successUi {
                val poi = POI(
                        null,
                        poiType.uuid,
                        region.id!!,
                        LatLng(it.latitude, it.longitude),
                        Date())
                Logger.v(poi.toString())
                POIStore.with(this).create(poi) successUi {
                    toast(R.string.poi_created)
                    (fragment_edit_area as EditRegionActivityFragment).addPOI(poi.copy(id = it))
                }
            }
        }
        assertPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION).successUi {
            permissionRequestHandlerMap[Manifest.permission.ACCESS_FINE_LOCATION]?.invoke()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionRequestHandlerMap[Manifest.permission.WRITE_EXTERNAL_STORAGE]?.invoke()
                } else {
                    toast("why not fake some poi types?")
                }
            REQUEST_ACCESS_FINE_LOCATION ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionRequestHandlerMap[Manifest.permission.ACCESS_FINE_LOCATION]?.invoke()
                }
            EditRegionActivityFragment.REQUEST_ACCESS_FINE_LOCATION ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragment_edit_area as EditRegionActivityFragment).onPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION,
                            EditRegionActivityFragment.REQUEST_ACCESS_FINE_LOCATION)
                }
        }
    }

    val center: LatLng
        get() = fragment_edit_area.map.map.cameraPosition.target
}





