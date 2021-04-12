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
    var onListChanged: () -> Unit = {}

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
        twUseUrl?.text = url
        useUrlView?.setOnClickListener { onClicked(SchoolInfo("",url, url)) }
        if (notify) {
            notifyDataSetChanged()
        }
    }

    private var useUrlView: View? = null
    private var twUseUrl: TextView? = null
    private var useUrlViewHolder: UseUrlViewHolder? = null

    private var statusView: View? = null
    private var loadingView: View? = null
    private var errorView: View? = null
    private var twErrorMessage: TextView? = null
    private var buttonRetry: Button? = null
    private var statusViewholder: StatusViewHolder? = null

    private fun updateStatusView(){
        if (status.status == ERROR){
            loadingView?.visibility = GONE
            errorView?.visibility = VISIBLE
            twErrorMessage?.text = context.getText(status.errMessage ?: R.string.unknown_error)
        }else if (status.status == LOADING){
            loadingView?.visibility = VISIBLE
            errorView?.visibility = GONE
        }else {
            loadingView?.visibility = GONE
            errorView?.visibility = GONE
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM){
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_school_info, viewGroup, false)
            return ItemViewHolder(itemView, onClicked)
        }
        if (viewType == TYPE_USE_URL){
            if (useUrlViewHolder == null){
                useUrlView = LayoutInflater.from(context).inflate(R.layout.item_use_url,viewGroup, false)
                twUseUrl = useUrlView!!.findViewById(R.id.textViewURL)
                useUrlViewHolder = UseUrlViewHolder(useUrlView!!)
            }else{

            }
            return useUrlViewHolder!!//UseUrlViewHolder(FrameLayout(context).apply { addView(TextView(context).apply { id = R.id.textView }) }, onClicked);
        }
        if(viewType == TYPE_STATUS){
            if (statusViewholder == null){
                statusView = LayoutInflater.from(context).inflate(R.layout.item_status,viewGroup, false)
                loadingView = statusView!!.findViewById(R.id.loadingLayout)
                errorView = statusView!!.findViewById(R.id.errorLayout)
                twErrorMessage = statusView!!.findViewById(R.id.textViewSchoolsError)
                buttonRetry = statusView!!.findViewById(R.id.buttonRetry)

                buttonRetry!!.setOnClickListener { retry() }
                updateStatusView()

                statusViewholder = StatusViewHolder(statusView!!)
            }
            return statusViewholder!!;
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
        println("item count: $toret")
        return toret
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

    inner class UseUrlViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    }
    inner class StatusViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    }
}