package com.puzheng.region_investigation

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import com.puzheng.region_investigation.model.POI
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.store.POITypeStore
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditPOIActivity : AppCompatActivity() {

    companion object {
        const val TAG_POI = "TAG_POI"
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 100
        const val REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE = 101
        const val SELECT_IMAGE = 1
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
                    resolve(it)?.apply {
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
                    container.addView(it.bind(data?.get(it.name)))
                }
            }
        }
    }

    private var outputFileUri: Uri? = null
    private val permisssionHandlers = mutableMapOf<Int, () -> Unit>()

    private fun resolve(field: POIType.Field) = when (field.type) {
        POIType.FieldType.STRING ->
            StringFieldResolver(field.name)
        POIType.FieldType.TEXT ->
            TextFieldResolver(field.name)
        POIType.FieldType.IMAGES ->

            ImagesFieldResolver(field.name, {
                permisssionHandlers[REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE] = {
                    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val cameraIntents = packageManager.queryIntentActivities(captureIntent, 0).map {
                        val packageName = it.activityInfo.packageName
                        Intent(captureIntent).apply {
                            intent.component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                            intent.`package` = packageName
                            //                            poi!!.dir.mkdirs()
                            //                            outputFileUri = Uri.fromFile(File.createTempFile(
                            //                                    SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()),
                            //                                    ".jpg",
                            //                                    poi!!.dir
                            //                            ))

                            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES), "MyCameraApp")
                            mediaStorageDir.mkdirs()
                            outputFileUri = Uri.fromFile(File.createTempFile(
                                    SimpleDateFormat("yyyyMMdd_HHmmss").format(Date()),
                                    ".jpg",
                                    mediaStorageDir
                            ).apply {
                                setWritable(true, false)
                            })
                            Logger.v(outputFileUri.toString())
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                            intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        }
                    }
                    val galleryIntent = Intent()
                    galleryIntent.type = "image/*"
                    galleryIntent.action = Intent.ACTION_GET_CONTENT
                    val chooserIntent = Intent.createChooser(galleryIntent, "选择图片来源")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                            cameraIntents.toTypedArray())
                    startActivityForResult(chooserIntent, SELECT_IMAGE)
                }
                // see http://stackoverflow.com/questions/4455558/allow-user-to-select-camera-or-gallery-for-image
                assertPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE) successUi {
                    permisssionHandlers[REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE]?.invoke()
                }
            })
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
            REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE ->
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    collectData() then {
                        permisssionHandlers[REQUEST_WRITE_EXTERNAL_STORAGE_SAVE_IMAGE]?.invoke()
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                val isCamera = (data == null ||
                        data.action == android.provider.MediaStore.ACTION_IMAGE_CAPTURE ||
                        data.action == "inline-data")
                if (!isCamera) {
                    contentResolver.openInputStream(data?.data).copyTo(File(outputFileUri!!.path))
                }
                (fieldResolvers.find {
                    it is ImagesFieldResolver
                } as ImagesFieldResolver).add(outputFileUri!!.path)
            } else if (resultCode == RESULT_CANCELED) {
                Logger.v(outputFileUri!!.path)
                File(outputFileUri!!.path).delete()
            }
        }
    }
}
