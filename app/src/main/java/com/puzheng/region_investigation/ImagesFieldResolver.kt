package com.puzheng.region_investigation

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ImagesFieldResolver(override val name: String, val poi: POI,
                          val onClickAddImage: (fieldResolver: ImagesFieldResolver) -> Unit,
                          val onClickImage: (fieldResolver: ImagesFieldResolver, images: List<String>, pos: Int) -> Unit) : FieldResolver {
    override fun changed(value: Any?): Boolean {
        val jsonArray = value as JSONArray? ?: return images.size > 0
        val set = images.toSet()
        return jsonArray.length() != images.size || (0..jsonArray.length() - 1).any {
            !set.contains(jsonArray.getString(it))
        }
    }

    private val picasso: Picasso by lazy {
        Picasso.with(MyApplication.context)
    }

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, JSONArray().apply {
            images.forEach {
                put(it)
            }
        })
    }

    private val view: View by lazy {
        View.inflate(MyApplication.context, R.layout.poi_field_images, null).apply {
            (findViewById(R.id.textViewFieldName) as TextView).text = name
        }
    }

    /**
     * 图片列表， 注意是信息点目录的相对目录
     */
    var images = mutableListOf<String>()

    private val recyclerView: RecyclerView by lazy {
        view.findViewById(R.id.recyclerView) as RecyclerView
    }

    override fun bind(value: Any?): View {
        if (value != null) {
            val jsonArray = value as JSONArray
            (0..jsonArray.length() - 1).forEach {
                images.add(jsonArray.getString(it))
            }
        }
        recyclerView.layoutManager = GridLayoutManager(view.context, 3)
        recyclerView.adapter = MyRecyclerViewAdapter()
        return view
    }


    companion object {
        private const val IMAGE = 0
        private const val ADD_IMAGE = 1
    }

    private inner class MyRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            if (position != 0) {
                val imageButton = (holder as ImageViewHolder).imageButton
                val image = images[position - 1]
                picasso.load(File(poi.dir, image)).fit().centerInside().into(imageButton)
                imageButton.setOnClickListener {
                    onClickImage(this@ImagesFieldResolver, images.map {
                        File(poi.dir, it).absolutePath
                    }, position - 1)
                }

            }
        }

        override fun getItemCount() = (1 + images.size)

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
            (view.findViewById(R.id.imageButton) as ImageButton).setOnClickListener {
                onClickAddImage(this@ImagesFieldResolver)
            }
        }
    }

    private class ImageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        lateinit var imageButton: ImageButton

        init {
            imageButton = view.findViewById(R.id.imageButton) as ImageButton
        }
    }

    /**
     * @param path 图片相对路径
     */
    fun add(path: String) {
        images.add(path)
        recyclerView.adapter.notifyDataSetChanged()
    }

    /**
     * @param images 图片相对路径列表
     */
    fun reset(images: List<String>?) {
        this.images.clear()
        images?.forEach {
            this.images.add(it)
        }
        recyclerView.adapter.notifyDataSetChanged()
    }

}
