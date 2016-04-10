package com.puzheng.area_investigation

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

fun View.findTextViewById(id: Int) = (findViewById(id) as TextView)
fun View.findImageViewById(id: Int) = (findViewById(id) as ImageView)
fun View.findListViewById(id: Int) = (findViewById(id) as ListView)
fun View.findCheckBoxById(id: Int) = (findViewById(id) as CheckBox)