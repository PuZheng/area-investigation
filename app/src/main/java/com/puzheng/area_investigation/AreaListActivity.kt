package com.puzheng.area_investigation

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import kotlinx.android.synthetic.main.activity_area_list.*
import kotlinx.android.synthetic.main.content_area_list.*

class AreaListActivity : AppCompatActivity(),
        AreaListFragment.OnAreaListFragmentInteractionListener {


    private var actionMode: ActionMode? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == CREATE_AREA || requestCode == EDIT_AREA) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                (supportFragmentManager.findFragmentById(R.id.fragmentAreaList) as AreaListFragment).fetchAreas()
            }
        }
    }

    override fun onLongClickItem(area: Area): Boolean {
        if (actionMode != null) {
            return false;
        }

        actionMode = startSupportActionMode(object : ModalMultiSelectorCallback((fragmentAreaList as AreaListFragment).multiSelector) {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_trash ->
                        TrashAlertDialogFragment().show(supportFragmentManager, "")
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                super.onCreateActionMode(mode, menu)
                mode?.menuInflater?.inflate(R.menu.context_menu_area_list, menu);
                fab.visibility = View.GONE
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                multiSelector.clearSelections()
                multiSelector.isSelectable = false
                fab.visibility = View.VISIBLE
                actionMode = null;
            }

        });
        return true;
    }

    override fun onClickItem(area: Area) {
        val intent = Intent(this, EditAreaActivity::class.java)
        intent.putExtra(TAG_AREA, area)
        startActivityForResult(intent, EDIT_AREA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init()

        setContentView(R.layout.activity_area_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener({
            val intent = Intent(this, CreateAreaActivity::class.java)
            startActivityForResult(intent, CREATE_AREA)
        })

        Logger.i(listOf("username: ${intent.getStringExtra("USERNAME")}",
                "org name: ${intent.getStringExtra("ORG_NAME")}",
                "org code: ${intent.getStringExtra("ORG_CODE")}").joinToString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_area_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val CREATE_AREA = 100
        private val EDIT_AREA = 100
        val TAG_AREA = "AREA"
    }
}

private class TrashAlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(R.string.warning).setMessage(R.string.trash_confirm_msg)
                .setPositiveButton(R.string.confirm, {
                    dialog, v ->
                    val fragment = (activity as AreaListActivity).fragmentAreaList as AreaListFragment
                    fragment.removeSelectedAreas()
                    fragment.multiSelector.clearSelections()
                }).setNegativeButton(R.string.cancel, null)
        // Create the AlertDialog object and return it
        return builder.create();
    }
}