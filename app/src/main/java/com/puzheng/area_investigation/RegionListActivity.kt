package com.puzheng.area_investigation

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Region
import kotlinx.android.synthetic.main.activity_area_list.*
import kotlinx.android.synthetic.main.content_area_list.*

class RegionListActivity : AppCompatActivity(),
        RegionListFragment.OnAreaListFragmentInteractionListener {


    private var actionMode: ActionMode? = null

    override fun onLongClickItem(region: Region): Boolean {
        if (actionMode != null) {
            return false;
        }

        actionMode = startSupportActionMode(object : ModalMultiSelectorCallback((fragmentAreaList as RegionListFragment).multiSelector) {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_trash ->
                        TrashAlertDialogFragment().show(supportFragmentManager, "")
                    R.id.action_upload ->
                            toast("尚未实现")
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

    override fun onClickItem(region: Region) {
        val intent = Intent(this, EditRegionActivity::class.java)
        intent.putExtra(TAG_AREA, region)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("AreaListActivity")
        setContentView(R.layout.activity_area_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener({
            val intent = Intent(this, CreateRegionActivity::class.java)
            startActivity(intent)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragmentAreaList as RegionListFragment).onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE)
                } else {
                    toast("why not fake some poi types?")
                }
        }
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
                    val fragment = (activity as RegionListActivity).fragmentAreaList as RegionListFragment
                    fragment.removeSelectedAreas()
                    fragment.multiSelector.clearSelections()
                }).setNegativeButton(R.string.cancel, null)
        // Create the AlertDialog object and return it
        return builder.create();
    }
}