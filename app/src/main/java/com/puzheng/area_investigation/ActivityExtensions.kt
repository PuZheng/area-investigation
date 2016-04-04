package com.puzheng.area_investigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import rx.Observable
import java.io.File

val Activity.pixelsPerDp: Double
    get() = resources.displayMetrics.densityDpi.toDouble() / DisplayMetrics.DENSITY_DEFAULT

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


fun Activity.assertPermission(permission: String, requestCode: Int) = Observable.create<Void> {
    val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
        it!!.onNext(null)
    } else {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // TODO Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.

        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission),
                    requestCode)
        }
    }
}