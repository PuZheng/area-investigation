package com.puzheng.region_investigation.model

import nl.komponents.kovenant.task
import org.json.JSONException
import org.json.JSONObject

data class POIType(val name: String, val timestamp: String, val fields: List<Field>,
                   val path: String) {
    fun extractPOIRawData(poi: POI) = task {
        if (poi.dataFile.exists()) {
            val json = JSONObject(poi.dataFile.readText())
            mapOf(*fields.map {
                it.name to try {
                    json.get(it.name)
                } catch (e: JSONException) {
                    null
                }
            }.toTypedArray())
        } else {
            null
        }
    }

    enum class FieldType {
        STRING, TEXT, VIDEO, IMAGES
    }

    data class Field(val name: String, val type: FieldType, val attrs: Map<String, String>?)
}
