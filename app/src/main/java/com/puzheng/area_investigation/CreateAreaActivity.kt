package com.puzheng.area_investigation

import android.app.Dialog
import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import com.amap.api.maps2d.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.databinding.ActivityCreateAreaBinding
import kotlinx.android.synthetic.main.activity_create_area.*

class CreateAreaActivity : AppCompatActivity(),
        CreateAreaStep1Fragment.OnFragmentInteractionListener,
        CreateAreaStep2Fragment.OnFragmentInteractionListener {

    override fun onDrawDone() {
        drawingActionMode?.finish()
    }

    override fun afterTextChanged(s: Editable?) {
        next.isEnabled = s.toString().isNotBlank()
    }

    private var drawingActionMode: ActionMode? = null

    override fun onMapLongClick(fragment: CreateAreaStep2Fragment, lnglat: LatLng) {
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

    lateinit private var binding: ActivityCreateAreaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("CreateAreaActivitiy")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_area)
        pager.adapter = object : FragmentStatePagerAdapter(supportFragmentManager) {

            private var fragments: MutableList<Fragment> = mutableListOf()

            init {
                fragments.add(CreateAreaStep1Fragment.newInstance())
                fragments.add(CreateAreaStep2Fragment.newInstance())
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
        binding.args = Args(ObservableField(false), ObservableField(true))

        supportActionBar?.title = getString(R.string.title_create_area)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prev.setOnClickListener({
            pager.currentItem -= 1
            binding.args.hasPrevious.set(pager.currentItem > 0)
            binding.args.hasNext.set(pager.currentItem < pager.adapter.count - 1)
        })
        next.setOnClickListener({
            pager.currentItem += 1
            binding.args.hasPrevious.set(pager.currentItem > 0)
            binding.args.hasNext.set(pager.currentItem < pager.adapter.count - 1)
        })
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
}

private class AffirmBackDialogFragment(val after: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(R.string.warning).setMessage(R.string.trash_cancel_create_area)
                .setPositiveButton(R.string.confirm, {
                    dialog, v ->
                    after()
                }).setNegativeButton(R.string.cancel, null)
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

private class CancelDrawingDialogFragment(val after: () -> Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(R.string.warning).setMessage(R.string.cancel_drawing)
                .setPositiveButton(R.string.confirm, {
                    dialog, v ->
                    after()
                }).setNegativeButton(R.string.cancel, null)
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

