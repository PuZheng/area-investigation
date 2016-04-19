package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.amap.api.maps.model.LatLng
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.store.POITypeStore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_poi_type.view.*

class POITypeChooseDialog(val poiTypes: List<POIType>, val after: (POIType) -> Unit) : AppCompatDialogFragment() {


    class ViewHolder(val textView: TextView, val imageView: ImageView)

    private val picasso: Picasso by lazy {
        Picasso.with(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(context).setTitle(R.string.pick_poi_type)
                    .setAdapter(object : BaseAdapter() {
                        private val store = POITypeStore.with(context)
                        override fun getItem(position: Int): Any? = poiTypes[position]

                        override fun getItemId(position: Int): Long = position.toLong()

                        override fun getCount(): Int = poiTypes.size

                        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? =
                                (convertView ?: getLayoutInflater(savedInstanceState).inflate(R.layout.item_poi_type, null)).apply {
                                    if (tag == null) {
                                        tag = ViewHolder(textViewName, imageViewIcon)
                                    }
                                    val item = getItem(position) as POIType
                                    val vh = tag as ViewHolder
                                    vh.textView.text = item.name
                                    picasso.load(store.getPOITypeIcon(item)).into(vh.imageView)
                                }
                    }, {
                        dialog, which ->
                        after(poiTypes[which])
                    }).create()
}
