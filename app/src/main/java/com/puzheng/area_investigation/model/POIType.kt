package com.puzheng.area_investigation.model

import nl.komponents.kovenant.task

data class POIType(val uuid: String, val name: String, val fields: List<Field>, val path: String) {
    fun extractPOIData(poi: POI) = task {
        mapOf<String, Any?>()
    }

    enum class FieldType {
        STRING, TEXT, VIDEO, IMAGES
    }

    data class Field(val name: String, val type: FieldType, val attrs: Map<String, String>?)
}
