package com.puzheng.area_investigation.model

import nl.komponents.kovenant.task
import org.json.JSONObject

data class POIType(val uuid: String, val name: String, val fields: List<Field>, val path: String) {
    fun extractPOIRawData(poi: POI) = task {
        if (poi.dataFile.exists()) {
            val json = JSONObject(poi.dataFile.readText())
            mapOf<String, Any?>(*fields.map {
                it.name to json.get(it.name)
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
