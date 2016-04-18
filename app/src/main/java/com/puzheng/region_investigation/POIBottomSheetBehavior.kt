package com.puzheng.region_investigation

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View

class POIBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

    constructor(): super() {
    }
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
    }
    override fun onLayoutChild(parent: CoordinatorLayout?, child: V, layoutDirection: Int): Boolean {
        val boolean = super.onLayoutChild(parent, child, layoutDirection)
        setState(STATE_EXPANDED)
        return boolean
    }
}