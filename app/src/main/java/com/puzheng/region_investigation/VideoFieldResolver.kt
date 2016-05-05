package com.puzheng.region_investigation

import android.content.Context
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
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
    override fun changed(value: Any?) = path != value

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

    private val imageView: ImageView by lazy {
        view.findView<ImageView>(R.id.imageView)
    }

    private val imageButton: ImageButton by lazy {
        view.findView<ImageButton>(R.id.imageButton)
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
                picasso.load(R.drawable.ic_play_circle_filled_white_36dp).into(imageButton)
                picasso.load(
                        File(poi.dir, value)).fit().centerInside().into(imageView)
            } else {
                imageView.setImageBitmap(null)
                picasso.load(R.drawable.ic_camera_pink_a200_48dp).into(imageButton)
            }
        }

    override fun bind(value: Any?): View {
        path = value as String?
        imageButton.setOnClickListener {
            onClick.invoke(this, path)
        }
        return view
    }
}