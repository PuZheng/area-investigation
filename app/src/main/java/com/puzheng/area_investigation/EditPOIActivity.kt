package com.puzheng.area_investigation

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.LinearLayout
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.POI
import com.puzheng.area_investigation.model.POIType
import com.puzheng.area_investigation.store.POITypeStore
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import java.text.SimpleDateFormat

class EditPOIActivity : AppCompatActivity() {

    companion object {
        const val TAG_POI = "TAG_POI"
    }
    private var poi: POI? = null
    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(this)
    }
    private val container: LinearLayout by lazy {
        findView<LinearLayout>(R.id.container)
    }


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
        poiTypeStore.get(poi!!.poiTypeUUID) then {
            if (it == null) {
                Logger.e("不存在该信息点类型 uuid ${poi!!.poiTypeUUID}")
                throw NoSuchPOIType()
            }
            it
        } then {
            poiType ->
            poiType!!.extractPOIData(poi!!) successUi {
                data ->
                poiType.fields.map {
                    resolve(it)?.bind(data[it.name]).apply {
                        if (this == null) {
                            toast("无法识别的字段, ${data[it.name]}")
                        }
                    }
                }.filter {
                    it != null
                }.forEach {
                    container.addView(it!!.view.apply {
                        Logger.v(toString())
                    })
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

}
