package com.puzheng.region_investigation

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import com.orhanobut.logger.Logger
import nl.komponents.kovenant.ui.promiseOnUi
import java.io.File

fun Activity.loadBitmap(resId: Int): Bitmap {
    val canvas = Canvas()
    val drawable = ContextCompat.getDrawable(this, resId)
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888)
    canvas.setBitmap(bitmap)
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    drawable.draw(canvas)
    return bitmap
}

fun Activity.loadBitmap(file: File): Bitmap = BitmapFactory.decodeStream(file.inputStream(), null,
        BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })


fun AppCompatActivity.assertPermission(permission: String, requestCode: Int, rationale: String? = null) = promiseOnUi {
    if (ContextCompat.checkSelfPermission(this@assertPermission, permission) != PackageManager.PERMISSION_GRANTED) {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this@assertPermission, permission) && rationale != null) {
            Logger.v("show rationale")
//            object : AppCompatDialogFragment() {
//                override fun onCreateDialog(savedInstanceState: Bundle?) =
//                        AlertDialog.Builder(context)..setMessage(rationale).create()
//            }.show(supportFragmentManager, "")
            // TODO Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
        } else {
            ActivityCompat.requestPermissions(this@assertPermission, arrayOf(permission),
                    requestCode)
        }
        throw Exception("permission $permission need to be affirmed")
    }
}

fun <T : Fragment?> AppCompatActivity.findFragmentById(id: Int) = supportFragmentManager.findFragmentById(id) as T

fun <T : View?> Activity.findView(id: Int) = findViewById(id) as T

fun AppCompatActivity.assertNetwork() = promiseOnUi {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val info = manager.activeNetworkInfo

    if (info == null || !info.isAvailable) {

        object: AppCompatDialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(this@assertNetwork).setTitle(R.string.warning).setMessage("网络不可访问，请前去设置")
                        .setPositiveButton(R.string.confirm, {
                           dialog, which ->
                            startActivity(
                                    if (Build.VERSION.SDK_INT > 10) {
                                        Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                    } else {
                                        Intent().apply {
                                            intent.component = ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
                                            intent.action = "android.intent.action.VIEW";
                                        }
                                    })
                        }).setNegativeButton(R.string.cancel, null).show()
            }
        }.show(supportFragmentManager, "");
        throw Exception("network unavailable")
    }
}
