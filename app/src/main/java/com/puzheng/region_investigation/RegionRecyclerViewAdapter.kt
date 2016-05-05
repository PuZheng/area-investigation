package com.puzheng.region_investigation

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
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
import com.puzheng.region_investigation.RegionListFragment.OnRegionListFragmentInteractionListener
import com.puzheng.region_investigation.model.Region
import com.puzheng.region_investigation.store.RegionStore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat

private val HEADER_TYPE = 1
private val REGION_TYPE = 2

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnRegionListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class RegionRecyclerViewAdapter(private var regions: List<Region?>?,
                                private val listener: OnRegionListFragmentInteractionListener,
                                private val multiSelector: MultiSelector) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val items = mutableListOf<Region?>()

    private val picasso: Picasso by lazy {
        Picasso.with(MyApplication.context)
    }


    init {
        setupItems()
    }

    private fun setupItems() {
        if (regions != null) {
            items.clear()
            for ((idx, region) in regions!!.withIndex()) {
                // 按天分组，如果不是同一天的，插入null，代表一个seperator
                if (idx == 0 || !region!!.created.ofSameDay(regions!![idx - 1]!!.created)) {
                    items.add(null)
                }
                items.add(region)
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
        REGION_TYPE
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        fun inflate(layout: Int) = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == HEADER_TYPE) {
            HeaderViewHolder(inflate(R.layout.fragment_region_header))
        } else {
            RegionViewHolder(inflate(R.layout.fragment_region), multiSelector, listener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val region = items[position]
        if (region == null) {
            val format = SimpleDateFormat("yy-MM-dd")
            (holder as HeaderViewHolder).textView.text = format.format(items[position + 1]!!.created)
        } else {
            (holder as RegionViewHolder).item = items[position]
            holder.textView.text = region.name
            // restore to default mode
            holder.textView.setTextColor(R.color.abc_primary_text_material_light)
            holder.imageView.background = null
            val context = holder.textView.context
            val coverFile = RegionStore.with(context).getCoverImageFile(region)
            picasso.load(coverFile).into(holder.imageView)
            if (region.isDirty) {
                holder.markAsDirty()
            }
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

    fun removeSelectedRegions() {
        regions = regions?.filter { it?.id !in selectedRegions.map { it.id } }
        Logger.v("""remained regions: ${regions?.map { it?.name }?.joinToString(",")}""")
        setupItems()
        notifyDataSetChanged()
    }


    private fun RegionViewHolder.markAsDirty() {
        imageView.background = ContextCompat.getDrawable(imageView.context, R.drawable.dirty_region_image)
        textView.text = "*" + textView.text
        textView.setTextColor(Color.RED)
    }
}

private class RegionViewHolder(val view: View, val multiSelector: MultiSelector, val listener: OnRegionListFragmentInteractionListener) :
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