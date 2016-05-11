package com.puzheng.region_investigation

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.puzheng.region_investigation.model.UploadTask

class UploadRecyclerViewAdapter(var tasks: List<Triple<UploadTask, Long, Long>>? = null) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val (task, sent, total) = tasks!![position]
        (holder as UploadTaskViewHolder).apply {
            textView.text = task.region!!.name
            if (total != 0L) {
                circularProgressBar.setProgressWithAnimation(sent.toFloat() / total)
            }
        }
    }

    override fun getItemCount() = (if (tasks == null) 0 else tasks!!.size)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? =
        UploadTaskViewHolder(
                LayoutInflater.from(parent?.context)
                        .inflate(R.layout.upload_task_item, parent, false))

    private class UploadTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val textView: TextView by lazy {
            itemView.findViewById(R.id.textViewRegionName) as TextView
        }

        val circularProgressBar: CircularProgressBar by lazy {
            itemView.findViewById(R.id.circularProgressBar) as CircularProgressBar
        }
    }
}
