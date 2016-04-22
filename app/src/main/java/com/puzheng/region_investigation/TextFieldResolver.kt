package com.puzheng.region_investigation

import android.content.Context

class TextFieldResolver(name: String, context: Context) : StringFieldResolver(name, context) {
    override val layoutId = R.layout.poi_field_text
}

