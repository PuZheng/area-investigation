package com.puzheng.region_investigation

import android.content.Context
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import org.json.JSONObject
import java.io.File


class VideoFieldResolver(override val name: String, context: Context, val poi: POI,
                         val onClick: (fieldResolver: VideoFieldResolver, path: String?) -> Unit) : FieldResolver {

    private val picasso: Picasso by lazy {
        Picasso.Builder(context).addRequestHandler(object : RequestHandler() {
            override fun canHandleRequest(data: Request?) =
                    ("file" == data?.uri?.scheme).apply {
                        Logger.v(data?.uri?.scheme)
                    }


            override fun load(request: Request?, networkPolicy: Int) =
                    Result(ThumbnailUtils.createVideoThumbnail(request?.uri?.path,
                            MediaStore.Images.Thumbnails.MINI_KIND), Picasso.LoadedFrom.DISK).apply {
                        Logger.v(request?.uri?.path)
                    }

        }).build()
    }

    private val imageButton: ImageButton by lazy {
        view.findView<ImageButton>(R.id.imageButton)
    }

    private val imageButtonPlay: ImageButton by lazy {
        view.findView<ImageButton>(R.id.imageButtonPlay)
    }

    override fun populate(jsonObject: JSONObject, poi: POI) {
        jsonObject.put(name, path)
    }

    private val view: View by lazy {
        View.inflate(context, R.layout.poi_field_video, null).apply {
            findView<TextView>(R.id.textViewFieldName).text = name

        }
    }

    // 注意，是相对路径
    var path: String? = null
        set(value) {
            field = value
            if (value != null) {
                imageButtonPlay.visibility = View.VISIBLE
                picasso.load(
                        File(poi.dir, value))
            } else {
                imageButtonPlay.visibility = View.GONE
                picasso.load(R.drawable.ic_camera_pink_a200_48dp)
            }.fit().centerInside().into(imageButton)
        }

    override fun bind(value: Any?): View {
        Logger.v("bind with `$value`")
        path = value as String?
        imageButton.setOnClickListener {
            onClick.invoke(this, path)
        }
        return view
    }
}