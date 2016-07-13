package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.POITypeStore
import com.squareup.picasso.Picasso
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.successUi

/**
 * 保留小数点后N位
 */
private fun roundOff(d: Double, n: Int): Double {
    Math.pow(10.0, n.toDouble()).let {
        return Math.round(d * it) / it
    }
}

class RegionStatDialogFragment(val region: Region) : AppCompatDialogFragment() {

    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(context)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView = View.inflate(context, R.layout.region_state_dialog, null)
        return AlertDialog.Builder(context).setTitle(R.string.region_stat_title).setView(contentView).create().apply {
            contentView.findTextViewById(R.id.textViewArea).text = roundOff(region.area / (1000 * 1000), 2).toString()
            task {
               region.poiListSync to poiTypeStore.listSync
            } successUi {
                val (pois, poiTypes) = it
                contentView.findTextViewById(R.id.textViewPOINO).text = if (pois != null) {
                    pois.size.toString()
                } else {
                    "0"
                }
                if (poiTypes == null) {
                    activity.toast("找不到信息点配置信息")
                } else {
                    val poiTypeMap = mapOf(*poiTypes.map {
                        it.name to it
                    }.toTypedArray())
                    var type2POINo = mutableMapOf<String, Int>()
                    pois?.forEach {
                        type2POINo.put(it.poiTypeName, type2POINo.getOrElse(it.poiTypeName, { 0 }) + 1)
                    }
                    type2POINo.entries.map {
                        poiTypeMap[it.key] to it.value
                    }.let {
                        contentView.findListViewById(R.id.listView).adapter = MyBaseAdaper(it)
                    }
                }
            }
        }
    }



    class ViewHolder(val textViewName: TextView, val imageViewIcon: ImageView, val textViewPOINO: TextView)

    private inner class MyBaseAdaper(val type_N_POINOList: List<Pair<POIType?, Int>>) : BaseAdapter() {

        private val picasso: Picasso by lazy {
            Picasso.with(context)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?) =
                (convertView ?: View.inflate(context, R.layout.item_poi_type_and_poi_no,
                        null)).apply {
                    if (tag == null) {
                        tag = ViewHolder(findTextViewById(R.id.textViewName), findImageViewById(R.id.imageViewIcon),
                                findTextViewById(R.id.textViewPOINO))
                    }
                    val (poiType, poiNO) = getItem(position)
                    val vh = tag as ViewHolder
                    if (poiType == null) {
                        vh.textViewName.text = getString(R.string.unknown_poi_type)
                    } else {
                        vh.textViewName.text = poiType.name
                        picasso.load(poiTypeStore.getPOITypeIcon(poiType)).into(vh.imageViewIcon)
                    }
                    vh.textViewPOINO.text = poiNO.toString()
                }

        override fun getItem(position: Int) = type_N_POINOList[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = type_N_POINOList.size

    }
}


