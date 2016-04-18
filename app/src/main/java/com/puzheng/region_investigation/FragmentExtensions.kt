package com.puzheng.region_investigation

import android.support.v4.app.Fragment
import android.util.DisplayMetrics

val Fragment.pixelsPerDp: Double
    get() = resources.displayMetrics.densityDpi.toDouble() / DisplayMetrics.DENSITY_DEFAULT


