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
        text = value as String
        return this
    }
}

class TextFieldResolver(val name: String): FieldResolver {
    override val view: View
        get() = throw UnsupportedOperationException()

    override fun bind(value: Any?): FieldResolver {
        throw UnsupportedOperationException()
    }
}

class ImagesFieldResolver(val name: String): FieldResolver {
    override val view: View
        get() = throw UnsupportedOperationException()

    override fun bind(value: Any?): FieldResolver {
        throw UnsupportedOperationException()
    }
}

class VideoFieldResolver(val name: String): FieldResolver {
    override val view: View
        get() = throw UnsupportedOperationException()

    override fun bind(value: Any?): FieldResolver {
        throw UnsupportedOperationException()
    }
}