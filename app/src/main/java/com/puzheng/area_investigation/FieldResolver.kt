package com.puzheng.area_investigation

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.EditText
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.POI
import org.json.JSONArray
import org.json.JSONObject

interface FieldResolver {

    fun bind(value: Any?): FieldResolver
    val view: View
    fun populate(jsonObject: JSONObject, poi: POI)
}

open class StringFieldResolver(val name: String) : FieldResolver {

    open protected val layoutId = R.layout.poi_field_string

    private val editText: TextInputEditText by lazy {
        view.findView<TextInputEditText>(R.id.editText)
    }

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, editText.text)
    }

    override val view: View by lazy {
        View.inflate(MyApplication.context, layoutId, null).apply {
            (this as TextInputLayout).hint = "请输入$name"
        }
    }

    private var text: String? = null

    override fun bind(value: Any?): FieldResolver {
        Logger.v("bind with `$value`")
        text = value as String?
        editText.setText(value)
        return this
    }
}

class TextFieldResolver(name: String) : StringFieldResolver(name) {
    override val layoutId = R.layout.poi_field_text
}

class ImagesFieldResolver(val name: String) : FieldResolver {
    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, JSONArray().apply {
            put("1.jpg")
            put("2.jpg")
            put("3.jpg")
            put("4.jpg")
            put("5.jpg")
        })
    }

    override val view: View by lazy {
        View.inflate(MyApplication.context, R.layout.poi_field_images, null)
    }


    private var images: List<String>? = null

    override fun bind(value: Any?): FieldResolver {
        Logger.v("bind with `$value`")
        if (value != null) {
            val jsonArray = value as JSONArray
            images = (0..jsonArray.length() - 1).map {
                jsonArray.getString(it)
            }
        }

        return this
    }
}

class VideoFieldResolver(val name: String) : FieldResolver {
    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, "1.mp4")
    }

    override val view: View by lazy {
        View.inflate(MyApplication.context, R.layout.poi_field_video, null)
    }

    private var path: String? = null

    override fun bind(value: Any?): FieldResolver {
        Logger.v("bind with `$value`")
        path = value as String?
        return this
    }
}