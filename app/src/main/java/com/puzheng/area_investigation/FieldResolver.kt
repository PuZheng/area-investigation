package com.puzheng.area_investigation

import android.view.View

interface FieldResolver {

    fun bind(value: Any?): FieldResolver
    val view: View
}

class StringFieldResolver(val name: String): FieldResolver {
    override val view: View = View.inflate(MyApplication.context, R.layout.poi_field_string, null)

    private var text: String? = null

    override fun bind(value: Any?): FieldResolver {
        text = value as String?
        return this
    }
}

class TextFieldResolver(val name: String): FieldResolver {
    override val view: View = View.inflate(MyApplication.context, R.layout.poi_field_text, null)

    private var text: String? = null

    override fun bind(value: Any?): FieldResolver {
        text = value as String?
        return this
    }
}

class ImagesFieldResolver(val name: String): FieldResolver {
    override val view: View = View.inflate(MyApplication.context, R.layout.poi_field_images, null)


    private var images: List<String>? = null

    override fun bind(value: Any?): FieldResolver {
        images = value as List<String>?
        return this
    }
}

class VideoFieldResolver(val name: String): FieldResolver {
    override val view: View = View.inflate(MyApplication.context, R.layout.poi_field_video, null)

    private var path: String? = null

    override fun bind(value: Any?): FieldResolver {
        path = value as String?
        return this
    }
}