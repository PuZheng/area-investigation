package com.puzheng.area_investigation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.widget.Toast

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

fun Activity.toast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Activity.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, resId, duration).show()
}
