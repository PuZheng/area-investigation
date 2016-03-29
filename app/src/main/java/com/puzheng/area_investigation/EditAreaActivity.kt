package com.puzheng.area_investigation

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.store.AreaStore
import kotlinx.android.synthetic.main.activity_edit_area.*
import kotlinx.android.synthetic.main.app_bar_edit_area_name.*
import rx.Observer
import rx.android.schedulers.AndroidSchedulers

class EditAreaActivity : AppCompatActivity() {

    lateinit private var area: Area

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_area)
        setSupportActionBar(toolbar)

        fab.setOnClickListener({ view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show() })
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
                                    .subscribe(object: Observer<Void> {
                                        override fun onCompleted() {
                                            toast(R.string.edit_area_name_success)
                                        }

                                        override fun onError(e: Throwable?) {
                                        }

                                        override fun onNext(t: Void) {
                                        }
                                    })
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
        else ->
            super.onOptionsItemSelected(item)
    }

    private fun updateContent() {
        supportActionBar!!.title = area.name
    }

}
