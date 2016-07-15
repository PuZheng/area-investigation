package com.puzheng.region_investigation

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.tool.reflection.SdkUtil
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.DialogFragment
import android.support.v4.text.TextUtilsCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ActionMode
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.github.lzyzsd.circleprogress.ArcProgress
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.Account
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.AccountStore
import com.puzheng.region_investigation.store.RegionStore
import kotlinx.android.synthetic.main.activity_region_list.*
import kotlinx.android.synthetic.main.content_region_list.*
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat

class RegionListActivity : AppCompatActivity(),
        RegionListFragment.OnRegionListFragmentInteractionListener {

    private var actionMode: ActionMode? = null
    private val regionStore: RegionStore by lazy {
        RegionStore.with(this)
    }

    private val arcProgress: ArcProgress by lazy {
        findViewById(R.id.arcProgress) as ArcProgress
    }

    private val mask: View by lazy {
        findViewById(R.id.mask) as View
    }

    private val regionListFragment: RegionListFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragmentRegionList) as RegionListFragment
    }

    override fun onLongClickItem(region: Region): Boolean {
        if (actionMode != null) {
            return false
        }
        actionMode = startSupportActionMode(supportActionMode)
        return true;
    }


    private val supportActionMode: ModalMultiSelectorCallback by lazy {
        object : ModalMultiSelectorCallback(
                (fragmentRegionList as RegionListFragment).multiSelector) {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.action_trash -> {
                        if (regionListFragment.selectedRegions.isNotEmpty()) {
                            TrashAlertDialogFragment().show(supportFragmentManager, "")
                        } else {
                            toast(R.string.select_at_least_one_region)
                        }
                    }
                    R.id.action_upload -> {
                        if (regionListFragment.selectedRegions.isEmpty()) {
                            toast(R.string.select_at_least_one_region)
                        } else {
                            arcProgress.apply {
                                progress = 0
                                bottomText = getString(R.string.generate_zip)
                            }
                            assertNetwork() successUi {
                                mask.visibility = View.VISIBLE
                                val selectRegions = regionListFragment.selectedRegions
                                task {
                                    selectRegions.withIndex().forEach {
                                        val (index, region) = it
                                        Logger.v("zip region ${region.name}")
                                        regionStore.generateZipSync(region)
                                        this@RegionListActivity.runOnUiThread {
                                            arcProgress.progress = (index + 1) * 100 / selectRegions.size
                                        }
                                    }
                                } successUi {
                                    val total = selectRegions.map {
                                        it.zipFile.length()
                                    }.sum()
                                    val df = DecimalFormat("#.#");
                                    df.roundingMode = RoundingMode.CEILING;
                                    arcProgress.bottomText = "正在上传(" + if (total < 1000) {
                                        "${df.format(total.toFloat()/1000)}K"
                                    } else {
                                        "${df.format(total.toFloat()/1000000)}M"
                                    } + ")"
                                    arcProgress.progress = 0
                                    total.toLong()
                                } then {
                                    val total = selectRegions.map {
                                        it.zipFile.length()
                                    }.sum()
                                    Logger.v("total bytes: $total")
                                    var sent = 0L
                                    try {
                                        selectRegions.forEach {
                                            regionStore.uploadSync(it) {
                                                this@RegionListActivity.runOnUiThread {
                                                    arcProgress.progress = ((it + sent) * 100 / total).toInt()
                                                }
                                            }
                                            sent += it.zipFile.length()
                                            it.setSyncedSync()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        throw e
                                    }
                                } failUi {
                                    object: AppCompatDialogFragment() {
                                        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                                            return AlertDialog.Builder(context).setIcon(R.drawable.ic_error_outline_red_300_18dp)
                                                    .setTitle("上传错误").setMessage("上传失败，请重试!").create()
                                        }
                                    }.show(supportFragmentManager, "")
                                } successUi {
                                    object: AppCompatDialogFragment() {
                                        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                                            return AlertDialog.Builder(context)
                                                    .setTitle(R.string.success)
                                                    .setMessage(R.string.upload_task_completed).create()
                                        }
                                    }.show(supportFragmentManager, "")
                                    regionListFragment.setupRegions()
                                } alwaysUi {
                                    mask.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                super.onCreateActionMode(mode, menu)
                mode?.menuInflater?.inflate(R.menu.context_menu_region_list, menu);
                fab.visibility = View.GONE
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                multiSelector.clearSelections()
                multiSelector.isSelectable = false
                fab.visibility = View.VISIBLE
                actionMode = null;
            }

        }
    }

    override fun onClickItem(region: Region) {
        startActivity(Intent(this, EditRegionActivity::class.java).apply {
            putExtra(TAG_REGION, region)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ConfigStore.with(applicationContext).configFile.exists()) {
            object: AppCompatDialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    return AlertDialog
                            .Builder(this@RegionListActivity)
                            .setMessage("请先拷贝config.json到目录" +
                                    File(Environment.getExternalStoragePublicDirectory(context.packageName), "").humanizePath)
                            .setPositiveButton("知道了", {
                                dialog, which ->
                                finish()
                            })
                            .create()
                }
            }.show(supportFragmentManager, "");
            return
        }
        Logger.init("RegionListActivity")
        setContentView(R.layout.activity_region_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            startActivity(Intent(this, CreateRegionActivity::class.java))
        }
        var username = intent.getStringExtra("USERNAME")
        var orgCode = intent.getStringExtra("ORG_CODE")
        var orgName = intent.getStringExtra("ORG_NAME")

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(orgCode) && !TextUtils.isEmpty(orgName)) {
            AccountStore.with(this).account = Account(username, orgCode, orgName)
        }

        if (AccountStore.with(this).account == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                assertPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_CONFIG_FILE).successUi {
                    setupAccount()
                }
            } else {
                setupAccount()
            }
        }
    }

    private fun setupAccount() {
        ConfigStore.with(this).let {
            val username = it.defaultUsername
            val orgCode = it.defaultOrgCode
            val orgName = it.defaultOrgName
            AccountStore.with(this).account = Account(username, orgCode, orgName)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_region_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, InfoActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        // 有两个OnPermissionGranted都要请求REQUEST_WRITE_EXTERNAL_STORAGE权限，那么只要赋予一次权限，就可以允许
        // 这两者都执行callback
        when (requestCode) {
            RegionListFragment.REQUEST_WRITE_EXTERNAL_STORAGE_TO_FAKE,
            MyApplication.REQUEST_WRITE_EXTERNAL_STORAGE_FOR_LOG ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    (fragmentRegionList as RegionListFragment).onPermissionGranted(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            requestCode)
                }
            REQUEST_READ_CONFIG_FILE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupAccount()
                }
        }
    }

    companion object {
        val TAG_REGION = "REGION"
        val REQUEST_READ_CONFIG_FILE = uniqueId()
    }
}

private class TrashAlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.warning).setMessage(R.string.trash_confirm_msg)
                .setPositiveButton(R.string.confirm, {
                    dialog, v ->
                    val fragment = (activity as RegionListActivity).fragmentRegionList as RegionListFragment
                    fragment.removeSelectedRegions()
                }).setNegativeButton(R.string.cancel, null)
        return builder.create();
    }
}