package com.puzheng.region_investigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ActionMode
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.databinding.ActivityCreateRegionBinding
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.activity_create_region.*
import kotlinx.android.synthetic.main.fragment_create_region_step1.*
import nl.komponents.kovenant.ui.successUi


class CreateRegionActivity : AppCompatActivity(),
        CreateRegionStep1Fragment.OnFragmentInteractionListener,
        CreateRegionStep2Fragment.OnFragmentInteractionListener {

    override fun onDrawDone(latLngList: List<LatLng>) {
        drawingActionMode?.finish()
        ConfirmCreateRegionDialog.newInstance(createRegionStep1Fragment.name.text.toString(),
                createRegionStep1Fragment.extras,
                latLngList).show(supportFragmentManager, "")
    }

    override fun afterTextChanged(s: Editable?, editText: EditText) {
        if (s.toString().isBlank()) {
            next.isEnabled = false
            return
        }
        RegionStore.with(this).uniqueName(s.toString()) successUi {
            next.isEnabled = it
            if (!it) {
                editText.error = getString(R.string.region_name_exists)
            } else {
                editText.error = null
            }
        }
    }

    private var drawingActionMode: ActionMode? = null

    override fun onMapLongClick(fragment: CreateRegionStep2Fragment, lnglat: LatLng) {
        if (drawingActionMode == null) {
            drawingActionMode = startSupportActionMode(object : ActionMode.Callback {
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false

                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                    fragment.startDraw()
                    fragment.addMarker(lnglat)
                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode?) {
                    fragment.stopDraw()
                    drawingActionMode = null
                }
            })
        }
    }

    lateinit private var binding: ActivityCreateRegionBinding

    private val createRegionStep1Fragment: CreateRegionStep1Fragment by lazy {
        CreateRegionStep1Fragment.newInstance()
    }
    private val createRegionStep2Fragment: CreateRegionStep2Fragment by lazy {
        CreateRegionStep2Fragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.sdcardDir = ConfigStore.with(this).offlineMapDataDir.path
        Logger.init("CreateRegionActivitiy")
        Logger.i(MapsInitializer.sdcardDir)
        val x: ViewDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_create_region)
        binding = x as ActivityCreateRegionBinding
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        pager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {

            private var fragments: MutableList<Fragment> = mutableListOf()

            init {
                fragments.add(createRegionStep1Fragment)
                fragments.add(createRegionStep2Fragment)
            }

            override fun getItem(position: Int): Fragment? = fragments[position]

            override fun getCount(): Int = 2

        }
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // 填写名称时要求展示输入法, 勾勒轮廓时隐藏输入法
                when (position) {
                    0 ->
                        imm.showSoftInput(currentFocus, 0)
                    1 ->
                        imm.hideSoftInputFromWindow(currentFocus.windowToken, 0);
                }
            }
        })

        supportActionBar?.title = getString(R.string.title_create_region)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prev.setOnClickListener {
            pager.currentItem -= 1
            binding.args.hasPrevious.set(pager.currentItem > 0)
            binding.args.hasNext.set(pager.currentItem < pager.adapter.count - 1)
        }

        next.setOnClickListener {
            pager.currentItem += 1
            binding.args.hasPrevious.set(pager.currentItem > 0)
            binding.args.hasNext.set(pager.currentItem < pager.adapter.count - 1)
        }
        binding.args = Args(ObservableField(false), ObservableField(true))
    }

    class Args(val hasPrevious: ObservableField<Boolean>, val hasNext: ObservableField<Boolean>)

    override fun onBackPressed() {
        AffirmBackDialogFragment({ super.onBackPressed() }).show(supportFragmentManager, "")
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else ->
            super.onContextItemSelected(item)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CreateRegionStep2Fragment.REQUEST_ACCESS_COARSE_LOCATION ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createRegionStep2Fragment.onPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION,
                            CreateRegionStep2Fragment.REQUEST_ACCESS_COARSE_LOCATION)
                }
        }
    }

}

private class AffirmBackDialogFragment(val after: () -> Unit) : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?) =
            AlertDialog.Builder(activity).setTitle(R.string.warning)
                    .setMessage(R.string.trash_cancel_create_region)
                    .setPositiveButton(R.string.confirm, {
                        dialog, v ->
                        after()
                    }).setNegativeButton(R.string.cancel, null).create()
}





