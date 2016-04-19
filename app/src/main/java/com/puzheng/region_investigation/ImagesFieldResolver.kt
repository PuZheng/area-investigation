package com.puzheng.region_investigation

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import org.json.JSONArray
import org.json.JSONObject

class ImagesFieldResolver(override val name: String, val onClickAddImage: () -> Unit) : FieldResolver {

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, JSONArray().apply {
            put("1.jpg")
            put("2.jpg")
            put("3.jpg")
            put("4.jpg")
            put("5.jpg")
        })
    }

    private val view: View by lazy {
        View.inflate(MyApplication.context, R.layout.poi_field_images, null)
    }

    private var images: List<String>? = null

    private val recyclerView: RecyclerView by lazy {
        view.findView<RecyclerView>(R.id.recyclerView)
    }

    override fun bind(value: Any?): View {
        Logger.v("bind with `$value`")
        if (value != null) {
            val jsonArray = value as JSONArray
            images = (0..jsonArray.length() - 1).map {
                jsonArray.getString(it)
            }
        }
        recyclerView.layoutManager = GridLayoutManager(view.context, 3)
        recyclerView.adapter = MyRecyclerView()

        return view
    }


    companion object {
        private const val IMAGE = 0
        private const val ADD_IMAGE = 1
    }

    private inner class MyRecyclerView: RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            if (position != 0) {

            }
        }

        override fun getItemCount(): Int {
            return 1 + (images?.size ?: 0)
        }

        override fun getItemViewType(position: Int) = if (position == 0) {
            ADD_IMAGE
        } else {
            IMAGE
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            if (viewType == ADD_IMAGE) {
                AddImageViewHolder(View.inflate(MyApplication.context, R.layout.poi_field_images_add_image,
                        null))
            } else {
                ImageViewHolder(View.inflate(MyApplication.context, R.layout.poi_field_images_image,
                        null))
            }

    }

    private inner class AddImageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.findView<ImageButton>(R.id.imageButton).setOnClickListener {
                onClickAddImage()
            }
        }
    }

    private class ImageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    }

    fun add(path: String) {
        Logger.v(path)
    }
}
