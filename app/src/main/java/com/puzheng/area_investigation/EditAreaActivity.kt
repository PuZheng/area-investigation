package com.puzheng.area_investigation

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.AreaStore
import com.puzheng.area_investigation.store.POITypeStore
import kotlinx.android.synthetic.main.activity_edit_area.*
import kotlinx.android.synthetic.main.app_bar_edit_area_name.*
import kotlinx.android.synthetic.main.content_edit_area.*
import rx.Observer
import rx.android.schedulers.AndroidSchedulers

class EditAreaActivity : AppCompatActivity(), EditAreaActivityFragment.OnFragmentInteractionListener {

    private var selectedVertex: LatLng? = null
    private var dataChanged = false

    override fun onMarkerSelected(position: LatLng) {
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
                (fragment_edit_area as EditAreaActivityFragment).editMode = true
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                editOutlineActionMode = null
                (fragment_edit_area as EditAreaActivityFragment).editMode = false
                fab?.show()
            }
        })

    }

    lateinit private var area: Area

    private fun fetchPOITypes(after: (List<POIType>) -> Unit) {
        POITypeStore.with(this).list.observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<List<POIType>?> {
            override fun onError(e: Throwable?) {
                if (e != null) throw e
            }

            override fun onNext(poiTypes: List<POIType>?) {
                if (poiTypes != null && poiTypes.isNotEmpty()) {
                    after(poiTypes)
                } else if (BuildConfig.DEBUG) {
                    POITypeStore.with(this@EditAreaActivity).fakeData().observeOn(AndroidSchedulers.mainThread()).subscribe {
                        fetchPOITypes({
                            after(it)
                        })
                    }
                } else {
                    this@EditAreaActivity.toast(R.string.no_poi_types)
                }
            }

            override fun onCompleted() {
            }
        })
        // TODO it should be polite to show a progressbar

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_area)
        setSupportActionBar(toolbar)
        toast(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).absolutePath)
        fab.setOnClickListener {
            fetchPOITypes {
                POITypeChooseDialog(it).show(supportFragmentManager, "")
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
}

private class POITypeChooseDialog(val poiTypes: List<POIType>) : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(context).setTitle(R.string.pick_poi_type)
                    .setAdapter(object : ArrayAdapter<POIType>(context, R.layout.item_poi_type, poiTypes.toTypedArray()) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
                            return super.getView(position, convertView, parent)
                        }
                    }, { dialog, which ->

                    }).create()
}





