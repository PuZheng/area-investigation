package com.puzheng.region_investigation

import android.view.View
import com.puzheng.region_investigation.model.POI
import org.json.JSONObject

interface FieldResolver {
    fun bind(value: Any?): View
    fun populate(jsonObject: JSONObject, poi: POI)
    fun changed(value: Any?): Boolean
    val name: String
}