package com.puzheng.area_investigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.amap.api.location.AMapLocation
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.model.POI
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.AreaStore
import com.puzheng.area_investigation.store.POIStore
import com.puzheng.area_investigation.store.POITypeStore
import kotlinx.android.synthetic.main.activity_edit_area.*
import kotlinx.android.synthetic.main.app_bar_edit_area_name.*
import kotlinx.android.synthetic.main.content_edit_area.*
import kotlinx.android.synthetic.main.fragment_edit_area.*
import kotlinx.android.synthetic.main.poi_bottom_sheet.*
import nl.komponents.kovenant.ui.successUi
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import java.util.*

private val REQUEST_WRITE_EXTERNAL_STORAGE = 100
private val REQUEST_ACCESS_FINE_LOCATION: Int = 101

class EditAreaActivity : AppCompatActivity(), EditAreaActivityFragment.OnFragmentInteractionListener {

    private val bottomSheetBehavior: BottomSheetBehavior<out View> by lazy {
        BottomSheetBehavior.from(design_bottom_sheet)
    }

    override fun onPOIMarkerSelected(marker: Marker?) {
        if (marker == null) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            fab?.visibility = View.VISIBLE
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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
                        (fragment_edit_area as EditAreaActivityFragment).deleteVertex(selectedVertex!!)
                    }
                    true
                }
                R.id.action_submit -> {
                    (fragment_edit_area as EditAreaActivityFragment).saveOutline({
                        editOutlineActionMode?.finish()
                    })
                    dataChanged = true
                    true
                }
                else -> false

            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.context_menu_edit_area_outline, menu);
                fab?.hide()
                (fragment_edit_area as EditAreaActivityFragment).editOutlineMode = true
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                editOutlineActionMode = null
                (fragment_edit_area as EditAreaActivityFragment).editOutlineMode = false
                fab?.show()
            }
        })

    }

    lateinit private var area: Area

    private val permissionRequestHandlerMap: MutableMap<String, () -> Unit> = mutableMapOf()

    private fun fetchPOITypes(after: (List<POIType>) -> Unit) {
        POITypeStore.with(this).list.observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<List<POIType>?> {
            override fun onError(e: Throwable?) {
                if (e != null) throw e
            }

            override fun onNext(poiTypes: List<POIType>?) {
                if (poiTypes != null && poiTypes.isNotEmpty()) {
                    after(poiTypes)
                } else {
                    val store = POITypeStore.with(this@EditAreaActivity)
                    this@EditAreaActivity.toast(resources.getString(R.string.no_poi_type_meta_info, store.dir.absolutePath), Toast.LENGTH_LONG)
                    permissionRequestHandlerMap[Manifest.permission.WRITE_EXTERNAL_STORAGE] = {
                        mkPOITypeDir()
                    }
                    assertPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            REQUEST_WRITE_EXTERNAL_STORAGE).successUi {
                        permissionRequestHandlerMap[Manifest.permission.WRITE_EXTERNAL_STORAGE]?.invoke()
                    }
                }
            }

            override fun onCompleted() {
            }
        })
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
        setSupportActionBar(toolbar)
        fab.setOnClickListener {
            fetchPOITypes {
                POITypeChooseDialog(it, { addPOI(it) })
                        .show(supportFragmentManager, "")
            }

        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)


        Logger.init("EditAreaActivity")
        area = intent.getParcelableExtra<Area>(AreaListActivity.TAG_AREA)
        Logger.v(area.toString())
        updateContent()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_area, menu)
        return true
    }

    private var editNameActionMode: ActionMode? = null

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.action_edit_name -> {
            editNameActionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    mode?.customView = layoutInflater.inflate(R.layout.app_bar_edit_area_name, null, false)
                    area_name.apply {
                        setText(area.name)
                        requestFocus()
                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(currentFocus, 0)
                        setSelection(area.name.length)
                    }
                    return true
                }

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                    when (item?.itemId) {
                        R.id.action_ok -> {
                            editNameActionMode!!.finish()
                            area.name = area_name.text.toString()
                            updateContent()
                            AreaStore.with(this@EditAreaActivity).updateAreaName(area.id!!, area_name.text.toString())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {
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
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else ->
            super.onOptionsItemSelected(item)
    }

    private fun updateContent() {
        supportActionBar!!.title = area.name
    }

    override fun onBackPressed() {
        // must assert that area.id is not null, note Long? is not Long
        if (dataChanged) setResult(Activity.RESULT_OK, Intent().apply { putExtra(TAG_AREA_ID, area.id!!) })
        super.onBackPressed()
    }

    companion object {
        val TAG_AREA_ID = "AREA_ID"

    }

    fun addPOI(poiType: POIType) {
        permissionRequestHandlerMap[Manifest.permission.ACCESS_FINE_LOCATION] = {
            getLocation(AMapLocation(Location("").apply {
                latitude = center.latitude
                longitude = center.longitude
            })).observeOn(AndroidSchedulers.mainThread()).subscribe {
                val poi = POI(
                        null,
                        poiType.uuid,
                        area.id!!,
                        LatLng(it.latitude, it.longitude),
                        Date())
                Logger.v(poi.toString())
                POIStore.with(this).create(poi).observeOn(AndroidSchedulers.mainThread()).subscribe {
                    toast(R.string.poi_created)
                    (fragment_edit_area as EditAreaActivityFragment).addPOI(poi.copy(id = it))
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
            EditAreaActivityFragment.REQUEST_ACCESS_FINE_LOCATION ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragment_edit_area as EditAreaActivityFragment).locate()
                }
        }
    }

    val center: LatLng
        get() = fragment_edit_area.map.map.cameraPosition.target
}





