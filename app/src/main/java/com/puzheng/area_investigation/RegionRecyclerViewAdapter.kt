package com.puzheng.area_investigation

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.RegionListFragment.OnAreaListFragmentInteractionListener
import com.puzheng.area_investigation.model.Region
import com.puzheng.area_investigation.store.RegionStore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat

private val HEADER_TYPE = 1
private val AREA_TYPE = 2

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnAreaListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class RegionRecyclerViewAdapter(private var regions: List<Region?>?,
                                private val listener: OnAreaListFragmentInteractionListener,
                                private val multiSelector: MultiSelector) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val items = mutableListOf<Region?>()

    var picasso: Picasso? = null

    init {
        setupItems()
    }

    private fun setupItems() {
        if (regions != null) {
            items.clear()
            for ((idx, area) in regions!!.withIndex()) {
                // 按天分组，如果不是同一天的，插入null，代表一个seperator
                if (idx == 0 || !area!!.created.ofSameDay(regions!![idx - 1]!!.created)) {
                    items.add(null)
                }
                items.add(area)
            }
        }
    }

    override fun getItemId(position: Int): Long = if (getItemViewType(position) == HEADER_TYPE) {
        super.getItemId(position)
    } else {
        items[position]!!.id!!
    }

    override fun getItemViewType(position: Int): Int = if (items[position] == null) {
        HEADER_TYPE
    } else {
        AREA_TYPE
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        fun inflate(layout: Int) = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        if (picasso == null) {
            picasso = Picasso.with(parent.context)
        }
        return if (viewType == HEADER_TYPE) {
            HeaderViewHolder(inflate(R.layout.fragment_area_header))
        } else {
            AreaViewHolder(inflate(R.layout.fragment_area), multiSelector, listener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val area = items[position]
        if (area == null) {
            val format = SimpleDateFormat("yy-MM-dd")
            (holder as HeaderViewHolder).textView.text = format.format(items[position + 1]!!.created)
        } else {
            (holder as AreaViewHolder).item = items[position]
            holder.textView.text = area.name
            val context = holder.textView.context
            val coverFile = RegionStore.with(context).getCoverImageFile(area)
            picasso?.load(coverFile)?.into(holder.imageView);
            Logger.v("bind ${area.name}")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class LayoutManager(context: Context, spanSize: Int) : GridLayoutManager(context, spanSize) {
        init {
            this.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (items[position] == null) {
                    2
                } else {
                    1
                }
            }
        }
    }

    val selectedRegions: List<Region>
        get() = (0..items.size - 1).filter {
            multiSelector.isSelected(it, 0)
        }.map {
            items[it]!!
        }

    fun removeSelectedAreas() {
        Logger.v("""selected areas: ${selectedRegions.map { it.name }.joinToString(",")}""")
        regions = regions?.filter { it?.id !in selectedRegions.map { it.id } }
        Logger.v("""remained areas: ${regions?.map { it?.name }?.joinToString(",")}""")
        setupItems()
        notifyDataSetChanged()
    }


}

private class AreaViewHolder(val view: View, val multiSelector: MultiSelector, val listener: OnAreaListFragmentInteractionListener) :
        SwappingHolder(view, multiSelector) {
    val textView: TextView
    val imageView: ImageView
    var item: Region? = null

    init {
        textView = view.findViewById(R.id.content) as TextView
        imageView = view.findViewById(R.id.imageView) as ImageView
        view.setOnClickListener {
            if (!multiSelector.tapSelection(this)) {
                listener.onClickItem(item!!)
            }
        }
        view.setOnLongClickListener {
            listener.onLongClickItem(item!!)
            multiSelector.isSelectable = true
            multiSelector.setSelected(this, true)
            true
        }
    }

    override fun toString(): String {
        return super.toString() + " '" + textView.text + "'"
    }


}

private class HeaderViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val textView: TextView

    init {
        textView = view.findViewById(R.id.text) as TextView
    }
}