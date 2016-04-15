package com.puzheng.area_investigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.POI
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.POITypeStore
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.json.JSONObject
import java.text.SimpleDateFormat

class EditPOIActivity : AppCompatActivity() {

    companion object {
        const val TAG_POI = "TAG_POI"
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 100
    }

    private var poi: POI? = null
    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(this)
    }
    private val container: LinearLayout by lazy {
        findView<LinearLayout>(R.id.container)
    }


    lateinit var fieldResolvers: List<FieldResolver>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("EditPOIActivity")
        setContentView(R.layout.activity_edit_poi)
        setSupportActionBar(findView<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        poi = if (savedInstanceState == null) {
            intent.getParcelableExtra<POI>(TAG_POI)
        } else {
            savedInstanceState.getParcelable(TAG_POI)
        }
        if (poi == null && BuildConfig.DEBUG) {
            // 伪造一个信息点用于调试
            val poiType = poiTypeStore.list.get()!![0]
            poi = POI(1L, poiType.uuid, 1L, randomHZLatLng,
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2016-03-09 12:32:23"))
        }
        findView<TextView>(R.id.textViewCreated).text =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(poi?.created)
        poiTypeStore.get(poi!!.poiTypeUUID) then {
            if (it == null) {
                Logger.e("不存在该信息点类型 uuid ${poi!!.poiTypeUUID}")
                throw NoSuchPOIType()
            }
            it
        } then {
            poiType ->
            findView<TextView>(R.id.textViewPOIType).text = poiType?.name
            poiType!!.extractPOIRawData(poi!!) successUi {
                data ->
                fieldResolvers = poiType.fields.map {
                    resolve(it)?.bind(data?.get(it.name)).apply {
                        if (this == null) {
                            toast("无法识别的字段, $it")
                        }
                    }
                }.filter {
                    it != null
                }.map {
                    it!!
                }
                fieldResolvers.forEach {
                    container.addView(it.view)
                }
            }
        }
    }

    private fun resolve(field: POIType.Field) = when (field.type) {
        POIType.FieldType.STRING ->
            StringFieldResolver(field.name)
        POIType.FieldType.TEXT ->
            TextFieldResolver(field.name)
        POIType.FieldType.IMAGES ->
            ImagesFieldResolver(field.name)
        POIType.FieldType.VIDEO ->
            VideoFieldResolver(field.name)
        else ->
            null
    }

    private fun collectData() = task {
        JSONObject().apply {
            fieldResolvers.forEach {
                it.populate(this, poi!!)
            }
        }.toString()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_edit_poi, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            true
        }
        R.id.action_submit -> {
            assertPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    REQUEST_WRITE_EXTERNAL_STORAGE) then {
                collectData() then {
                    poi?.saveData(it)?.successUi {
                        toast(R.string.poi_data_saved)
                    }
                }
            }

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_STORAGE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    collectData() then {
                        poi?.saveData(it)?.successUi {
                            toast(R.string.poi_data_saved)
                        }
                    }
                }
        }
    }
}
