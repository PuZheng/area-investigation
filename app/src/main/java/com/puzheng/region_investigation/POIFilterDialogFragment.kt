package com.puzheng.region_investigation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POIType
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.POITypeStore
import com.puzheng.region_investigation.store.RegionStore
import com.squareup.picasso.Picasso
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi

/**
 * 过滤信息点对话框
 */
class POIFilterDialogFragment : AppCompatDialogFragment() {

    companion object {
        const val ARG_REGION = "ARG_REGION"
        const val ARG_HIDDEN_POI_TYPES = "ARG_HIDDEN_POI_TYPES"

        fun newInstance(region: Region, hiddenPOITypes: List<POIType>): POIFilterDialogFragment {
            val d = POIFilterDialogFragment()
            d.arguments = Bundle()
            d.arguments.putParcelable(ARG_REGION, region)
            d.arguments.putParcelableArray(ARG_HIDDEN_POI_TYPES, hiddenPOITypes.toTypedArray())
            return d
        }
    }

    private lateinit var region: Region
    private lateinit var hiddenPOITypes: List<POIType>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        region = arguments.getParcelable(ARG_REGION)
        hiddenPOITypes = listOf(*arguments.getParcelableArray(ARG_HIDDEN_POI_TYPES)).map {
            it as POIType
        }
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
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val d = AlertDialog.Builder(activity).setTitle(R.string.poi_filter_dialog_title).setView(contentView)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    listener?.onFilterPOI(hiddenPOITypes)
                }).setNegativeButton(R.string.cancel, null).create()
        task {
            poiTypeStore.listSync to region.poiListSync
        } successUi {
            val (poiTypes, pois)  = it
            if (poiTypes == null) {
                activity.toast("没有信息点类型信息, 请将信息点配置文件拷贝到" + POITypeStore.with(activity).dir.humanizePath)
            } else if (pois != null) {
                // 过滤掉所有当前区域没有的信息点类型
                setOf(*pois.map {
                    it.poiTypeName
                }.toTypedArray()).let {
                    poiTypeNameSet ->
                    (contentView as ListView).adapter = MyBaseAdapger(poiTypes.filter {
                        poiTypeNameSet.contains(it.name)
                    })
                }

            }
        }
        return d
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
                                }
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
        fun onFilterPOI(hiddenPOITypes: List<POIType>)
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
