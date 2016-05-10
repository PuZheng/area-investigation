package com.puzheng.region_investigation

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.puzheng.region_investigation.model.Region

class UploadRecyclerViewAdapter(var tasks: List<Triple<Region, Int, Int>>? = null) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun getItemCount() = if (tasks == null) 0 else tasks!!.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        throw UnsupportedOperationException()
    }

}
