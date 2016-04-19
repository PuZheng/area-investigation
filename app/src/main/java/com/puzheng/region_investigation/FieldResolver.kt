package com.puzheng.region_investigation

import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.View
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import org.json.JSONObject

interface FieldResolver {
    fun bind(value: Any?): View
    fun populate(jsonObject: JSONObject, poi: POI)
    val name: String

}

open class StringFieldResolver(override val name: String) : FieldResolver {

    open protected val layoutId = R.layout.poi_field_string

    private val editText: TextInputEditText by lazy {
        view.findView<TextInputEditText>(R.id.editText)
    }

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, editText.text)
    }

    private val view: View by lazy {
        View.inflate(MyApplication.context, layoutId, null).apply {
            (this as TextInputLayout).hint = "请输入$name"
        }
    }

    private var text: String? = null

    override fun bind(value: Any?): View {
        Logger.v("bind with `$value`")
        text = value as String?
        editText.setText(value)
        return view
    }
}

class TextFieldResolver(name: String) : StringFieldResolver(name) {
    override val layoutId = R.layout.poi_field_text
}


class VideoFieldResolver(override val name: String) : FieldResolver {
    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, "1.mp4")
    }

    private val view: View by lazy {
        View.inflate(MyApplication.context, R.layout.poi_field_video, null)
    }

    private var path: String? = null

    override fun bind(value: Any?): View {
        Logger.v("bind with `$value`")
        path = value as String?
        return view
    }
}