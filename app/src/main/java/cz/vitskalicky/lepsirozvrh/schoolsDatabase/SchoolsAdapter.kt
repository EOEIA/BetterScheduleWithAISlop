package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.StatusInfo
import cz.vitskalicky.lepsirozvrh.model.StatusInfo.Status.*

class SchoolsAdapter(private val context: Context, private val onClicked: (SchoolInfo) -> Unit, private val retry: () -> Unit) : PagedListAdapter<SchoolInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    var onListChanged: (previousList: PagedList<SchoolInfo>?, currentList: PagedList<SchoolInfo>?) -> Unit = { _, _ -> }

    public var status: StatusInfo = StatusInfo.unknown()
    set(value) {
        var notify = showLoadingOrError()
        field = value
        notify = notify != showLoadingOrError()
        updateStatusView()
        if (notify) {
            notifyDataSetChanged()
        }
    }
    public var queryText: String = ""
    set(value) {
        var notify = showUseUrl()
        field = value
        notify = notify != showUseUrl()
        //todo notify unencrypted is not possible on release
        val url = if (field.startsWith("https://") || field.startsWith("http://")) field else "https://$field"
        useUrlViewHolders.forEach {
            it.twUseUrl.text = url
            it.view.setOnClickListener { onClicked(SchoolInfo("",url, url)) }
        }
        if (notify) {
            notifyDataSetChanged()
        }
    }

    private var useUrlViewHolders: HashSet<UseUrlViewHolder> = HashSet()

    private var statusViewholders: HashSet<StatusViewHolder> = HashSet()

    private fun updateStatusView(){
        if (status.status == ERROR){
            statusViewholders.forEach {
                it.loadingView.visibility = GONE
                it.errorView.visibility = VISIBLE
                it.twErrorMessage.text = context.getText(status.errMessage ?: R.string.unknown_error)
            }
        }else if (status.status == LOADING){
            statusViewholders.forEach {
                it.loadingView.visibility = VISIBLE
                it.errorView.visibility = GONE
            }
        }else {
            statusViewholders.forEach {
                it.loadingView.visibility = GONE
                it.errorView.visibility = GONE
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM){
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_school_info, viewGroup, false)
            return ItemViewHolder(itemView, onClicked)
        }
        if (viewType == TYPE_USE_URL){
            val useUrlViewHolder = UseUrlViewHolder(LayoutInflater.from(context).inflate(R.layout.item_use_url,viewGroup, false))
            queryText = queryText
            useUrlViewHolders.add(useUrlViewHolder)
            return useUrlViewHolder
        }
        if(viewType == TYPE_STATUS){
            val statusViewholder = StatusViewHolder(LayoutInflater.from(context).inflate(R.layout.item_status,viewGroup, false))
            statusViewholders.add(statusViewholder)
            updateStatusView()
            return statusViewholder;
        }

        throw IllegalStateException("WTF? Unknown view type in recycler view. This is really not supposed to happen.");
    }

    override fun onCurrentListChanged(previousList: PagedList<SchoolInfo>?, currentList: PagedList<SchoolInfo>?) {
        super.onCurrentListChanged(previousList, currentList)
        onListChanged(previousList, currentList)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            TYPE_ITEM -> {
                require(holder is ItemViewHolder)
                val item = getItem(position)
                holder.bind(item)
            }
            TYPE_USE_URL, TYPE_STATUS -> {
                //dont do anything
            }
        }

    }

    private fun showLoadingOrError(): Boolean {
        return status.status == LOADING || status.status == ERROR
    }

    private fun showUseUrl(): Boolean {
        return queryText.isNotBlank()
    }

    override fun getItemCount(): Int {
        // +1 for "use this as url"
        val toret = super.getItemCount() + (if (showLoadingOrError()) {1} else {0}) + (if (showUseUrl()) {1} else {0})
        return toret
    }

    override fun getItemViewType(position: Int): Int {
        val itemCount = super.getItemCount()
        return when{
            position < itemCount -> TYPE_ITEM
            position == itemCount -> if (showUseUrl()) TYPE_USE_URL else TYPE_STATUS
            position == itemCount + 1 -> TYPE_STATUS
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

    inner class UseUrlViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val twUseUrl: TextView = view.findViewById(R.id.textViewURL)
    }
    inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val loadingView: View = view.findViewById(R.id.loadingLayout)
        val errorView: View = view.findViewById(R.id.errorLayout)
        val twErrorMessage: TextView = view.findViewById(R.id.textViewSchoolsError)
        val buttonRetry: Button = view.findViewById(R.id.buttonRetry)

        init {
            buttonRetry.setOnClickListener { retry() }
        }
    }
}