package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.vitskalicky.lepsirozvrh.R

class SchoolsAdapter(private val context: Context, private val onClicked: (SchoolInfo) -> Unit) : PagedListAdapter<SchoolInfo, SchoolsAdapter.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_school_info, viewGroup, false)
        return ViewHolder(itemView, onClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(val view: View, val onClicked: (SchoolInfo) -> Unit) : RecyclerView.ViewHolder(view) {
        val twName: TextView  = view.findViewById(R.id.textViewName)
        val twURL: TextView  = view.findViewById(R.id.textViewURL)
        var item: SchoolInfo? = null

        fun bind(item: SchoolInfo?) {
            this.item = item
            if (item == null) {
                clear()
            } else {
                twName.text = item.name
                twURL.text = item.url
                view.setOnClickListener { v: View? -> onClicked(item) }
            }
        }

        fun clear() {
            item = null
            twName.text = ""
            twURL.text = ""
            view.setOnClickListener { v: View? -> }
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<SchoolInfo> = object : DiffUtil.ItemCallback<SchoolInfo>() {
            override fun areItemsTheSame(oldItem: SchoolInfo, newItem: SchoolInfo): Boolean {
                // The ID property identifies when items are the same.
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SchoolInfo, newItem: SchoolInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}