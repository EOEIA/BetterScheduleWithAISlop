package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import java.util.*

class SchoolsAdapter(private val context: Context, private val onClicked: (SchoolInfo) -> Unit, private val retry: () -> Unit) : PagedListAdapter<SchoolInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    var onListChanged: () -> Unit = {}

    public var status: StatusInfo = StatusInfo.unknown();
    public var queryText: String = ""
    set(value) {
        var notify = showUseUrl()
        field = value
        notify = notify != showUseUrl()
        //todo R.string
        //todo notify unencrypted is not possible on release
        val url = if (field.startsWith("https://") || field.startsWith("http://")) field else "https://$field"
        twUseUrl.text = "Use \"$url\""
        twUseUrl.setOnClickListener { onClicked(SchoolInfo("",/*TODO*/"dfdsf", url)) }
        if (notify) {
            notifyDataSetChanged()
        }
    }

    private val twUseUrl = TextView(context)
    private val useUrlViewHolder = UseUrlViewHolder(twUseUrl)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM){
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_school_info, viewGroup, false)
            return ItemViewHolder(itemView, onClicked)
        }
        if (viewType == TYPE_USE_URL){
            return useUrlViewHolder//UseUrlViewHolder(FrameLayout(context).apply { addView(TextView(context).apply { id = R.id.textView }) }, onClicked);
        }

        throw IllegalStateException("WTF? Unknown view type in recycler view. This is really not supposed to happen.");
    }

    override fun onCurrentListChanged(previousList: PagedList<SchoolInfo>?, currentList: PagedList<SchoolInfo>?) {
        super.onCurrentListChanged(previousList, currentList)
        onListChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            TYPE_ITEM -> {
                require(holder is ItemViewHolder)
                val item = getItem(position)
                holder.bind(item)
            }
            TYPE_USE_URL -> {
                /*require(holder is UseUrlViewHolder)
                holder.bind(queryText)
                useUrlviewholders.add(holder)*/
            }
        }

    }

    private fun showLoadingOrError(): Boolean {
        return false//status.status == StatusInfo.Status.LOADING || status.status == StatusInfo.Status.ERROR
    }

    private fun showUseUrl(): Boolean {
        return queryText.isNotBlank()
    }

    override fun getItemCount(): Int {
        // +1 for "use this as url"
        return super.getItemCount() + if (showLoadingOrError()) 1 else 0 + if (showUseUrl()) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        val itemCount = super.getItemCount()
        return when{
            position < itemCount -> TYPE_ITEM
            position == itemCount -> if (showLoadingOrError()) TYPE_STATUS else TYPE_USE_URL
            position == itemCount + 1 -> TYPE_USE_URL
            else -> throw IllegalStateException("WTF? Too many items in list??? This is really not supposed to happen.");
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

        const val TYPE_ITEM = 0;
        const val TYPE_STATUS = 1;
        const val TYPE_USE_URL = 2;
    }

    // VIEW HOLDERS

    inner class ItemViewHolder(val view: View, val onClicked: (SchoolInfo) -> Unit) : RecyclerView.ViewHolder(view) {
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
                view.setOnClickListener {
                    v: View? -> onClicked(item)
                }
            }
        }

        fun clear() {
            item = null
            twName.text = ""
            twURL.text = ""
            view.setOnClickListener { v: View? -> }
        }
    }

    inner class UseUrlViewHolder(val view: View/*, val onClicked: (SchoolInfo) -> Unit*/) : RecyclerView.ViewHolder(view) {
        /*val tw: TextView  = view.findViewById(R.id.textView)

        fun bind(url: String) {
            tw.text = "Use \"$url\"" //todo R.string
            view.setOnClickListener { v: View? -> onClicked()}
        }*/
    }
}