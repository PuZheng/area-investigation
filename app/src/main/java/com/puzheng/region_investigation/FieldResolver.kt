package com.puzheng.region_investigation

import android.content.Context
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import org.json.JSONObject

interface FieldResolver {
    fun bind(value: Any?): View
    fun populate(jsonObject: JSONObject, poi: POI)
    val name: String

}

open class StringFieldResolver(override val name: String, val context: Context) : FieldResolver {

    open protected val layoutId = R.layout.poi_field_string

    private val editText: EditText by lazy {
        view.findView<EditText>(R.id.editText)
    }

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, editText.text)
    }

    private val view: View by lazy {
        View.inflate(context, layoutId, null).apply {
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

class TextFieldResolver(name: String, context: Context) : StringFieldResolver(name, context) {
    override val layoutId = R.layout.poi_field_text
}