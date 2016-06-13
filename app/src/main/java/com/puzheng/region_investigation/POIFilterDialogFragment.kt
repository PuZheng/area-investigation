package com.puzheng.region_investigation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.POITypeStore
import com.puzheng.region_investigation.store.RegionStore
import com.squareup.picasso.Picasso
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.ui.successUi

/**
 * 过滤信息点对话框
 */
class POIFilterDialogFragment(val region: Region, var hiddenPOITypes: Set<POIType>) : AppCompatDialogFragment() {

    private val regionStore: RegionStore by lazy {
        RegionStore.with(context)
    }

    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(context)
    }

    private val contentView: View by lazy {
        ListView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                val verticalPadding = (32 * activity.pixelsPerDp).toInt()
                val horizontalPadding = (32 * activity.pixelsPerDp).toInt()
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            }

            poiTypeStore.list and region.poiList successUi {
                val (poiTypes, pois) = it
                if (poiTypes == null) {
                    activity.toast("没有信息点类型信息, 请将信息点配置文件拷贝到" + POITypeStore.with(activity).dir.humanizePath)
                } else if (pois != null) {
                    // 过滤掉所有当前区域没有的信息点类型
                    setOf(*pois.map {
                        it.poiTypeName
                    }.toTypedArray()).let {
                        poiTypeNameSet ->
                        adapter = MyBaseAdapger(poiTypes.filter {
                            poiTypeNameSet.contains(it.name)
                        })
                    }

                }
            }
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity).setTitle(R.string.poi_filter_dialog_title).setView(contentView)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    listener?.onFilterPOI(hiddenPOITypes)
                }).setNegativeButton(R.string.cancel, null).create()
    }

    private class ViewHolder(val textView: TextView, val imageView: ImageView, val checkBox: CheckBox)

    private inner class MyBaseAdapger(val poiTypes: List<POIType>) : BaseAdapter() {

        private val picasso: Picasso by lazy {
            Picasso.with(context)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? =
                (convertView ?: View.inflate(context, R.layout.item_poi_filter, null)).apply {
                    if (tag == null) {
                        tag = ViewHolder(findTextViewById(R.id.textView), findImageViewById(R.id.imageView),
                                findCheckBoxById(R.id.checkBox))
                    }
                    val vh = tag as ViewHolder
                    getItem(position).let {
                        item ->
                        vh.textView.text = item.name
                        picasso.load(poiTypeStore.getPOITypeIcon(item)).into(vh.imageView)
                        vh.checkBox.setOnCheckedChangeListener { compoundButton, b ->
                            if (b) {
                                hiddenPOITypes = hiddenPOITypes.filter {
                                    it.name != item.name
                                }.toSet()
                            } else {
                                hiddenPOITypes = hiddenPOITypes.plus(item)
                            }
                        }
                        vh.checkBox.isChecked = !hiddenPOITypes.any { it.name == item.name }
                    }
                }

        override fun getItem(position: Int) = poiTypes[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = poiTypes.size
    }

    interface OnFragmentInteractionListener {
        fun onFilterPOI(hiddenPOITypes: Set<POIType>)
    }

    private var listener: OnFragmentInteractionListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
