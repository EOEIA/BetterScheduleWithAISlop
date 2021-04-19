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

/**
 * @param onClicked the boolean says whether the url has been selected from list of known schools or selected manually.
 */
class SchoolsAdapter(private val context: Context,
                     private val onClicked: (SchoolInfo) -> Unit,
                     private val retry: () -> Unit,
                     /**
                      * this wll be called when the user wants to enter url manually. The string is what the user has entered so far.*/
                     private val onShowManualUrlDialog: (String) -> Unit
) : PagedListAdapter<SchoolInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
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
    /* this error rarely occurred and I could not reproduce it. I should be fixed, but since I could not reproduce it, you can never be sure.

    E/AndroidRuntime: FATAL EXCEPTION: main
    Process: cz.vitskalicky.lepsirozvrh, PID: 32503
    java.lang.IllegalArgumentException: Called attach on a child which is not detached: UseUrlViewHolder{f0ed70f position=1 id=-1, oldPos=-1, pLpos:5 no parent} androidx.recyclerview.widget.RecyclerView{76639d9 VFED..... ......I. 0,184-1080,1334 #7f09013d app:id/recyclerView}, adapter:cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsAdapter@487648b, layout:androidx.recyclerview.widget.LinearLayoutManager@cca5968, context:cz.vitskalicky.lepsirozvrh.activity.SchoolsListActivity@31d8bf3
        at androidx.recyclerview.widget.RecyclerView$5.attachViewToParent(RecyclerView.java:920)
        at androidx.recyclerview.widget.ChildHelper.attachViewToParent(ChildHelper.java:239)
        at androidx.recyclerview.widget.RecyclerView$LayoutManager.addViewInt(RecyclerView.java:8582)
        at androidx.recyclerview.widget.RecyclerView$LayoutManager.addView(RecyclerView.java:8559)
        at androidx.recyclerview.widget.RecyclerView$LayoutManager.addView(RecyclerView.java:8547)
        at androidx.recyclerview.widget.LinearLayoutManager.layoutChunk(LinearLayoutManager.java:1641)
        at androidx.recyclerview.widget.LinearLayoutManager.fill(LinearLayoutManager.java:1587)
        at androidx.recyclerview.widget.LinearLayoutManager.onLayoutChildren(LinearLayoutManager.java:665)
        at androidx.recyclerview.widget.RecyclerView.dispatchLayoutStep1(RecyclerView.java:4085)
        at androidx.recyclerview.widget.RecyclerView.dispatchLayout(RecyclerView.java:3849)
        at androidx.recyclerview.widget.RecyclerView.onLayout(RecyclerView.java:4404)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at androidx.constraintlayout.widget.ConstraintLayout.onLayout(ConstraintLayout.java:1855)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at androidx.constraintlayout.widget.ConstraintLayout.onLayout(ConstraintLayout.java:1855)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
        at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
        at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
        at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.widget.LinearLayout.setChildFrame(LinearLayout.java:1812)
        at android.widget.LinearLayout.layoutVertical(LinearLayout.java:1656)
        at android.widget.LinearLayout.onLayout(LinearLayout.java:1565)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.widget.FrameLayout.layoutChildren(FrameLayout.java:323)
        at android.widget.FrameLayout.onLayout(FrameLayout.java:261)
        at com.android.internal.policy.DecorView.onLayout(DecorView.java:1041)
        at android.view.View.layout(View.java:22419)
        at android.view.ViewGroup.layout(ViewGroup.java:6584)
        at android.view.ViewRootImpl.performLayout(ViewRootImpl.java:3378)
        at android.view.ViewRootImpl.performTraversals(ViewRootImpl.java:2842)
        at android.view.ViewRootImpl.doTraversal(ViewRootImpl.java:1888)
        at android.view.ViewRootImpl$TraversalRunnable.run(ViewRootImpl.java:8511)
        at android.view.Choreographer$CallbackRecord.run(Choreographer.java:949)
        at android.view.Choreographer.doCallbacks(Choreographer.java:761)
        at android.view.Choreographer.doFrame(Choreographer.java:696)
        at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:935)
        at android.os.Handler.handleCallback(Handler.java:873)
E/AndroidRuntime:     at android.os.Handler.dispatchMessage(Handler.java:99)
        at android.os.Looper.loop(Looper.java:214)
        at android.app.ActivityThread.main(ActivityThread.java:7050)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:494)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:965)

    * */

    public var queryText: String = ""
        set(value) {
            var notify = showUseUrl()
            field = value
            notify = notify != showUseUrl()
            //todo notify unencrypted is not possible on release
            useUrlViewHolders.forEach {
                //it.twUseUrl.text = url
                it.view.setOnClickListener { onShowManualUrlDialog(value) }
            }
            if (notify) {
                notifyDataSetChanged()
            }
        }

    private var useUrlViewHolders: HashSet<UseUrlViewHolder> = HashSet()

    private var statusViewholders: HashSet<StatusViewHolder> = HashSet()

    private fun updateStatusView() {
        if (status.status == ERROR) {
            statusViewholders.forEach {
                it.loadingView.visibility = GONE
                it.errorView.visibility = VISIBLE
                it.twErrorMessage.text = context.getText(status.errMessage ?: R.string.unknown_error)
            }
        } else if (status.status == LOADING) {
            statusViewholders.forEach {
                it.loadingView.visibility = VISIBLE
                it.errorView.visibility = GONE
            }
        } else {
            statusViewholders.forEach {
                it.loadingView.visibility = GONE
                it.errorView.visibility = GONE
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_school_info, viewGroup, false)
            return ItemViewHolder(itemView, onClicked)
        }
        if (viewType == TYPE_USE_URL) {
            val useUrlViewHolder = UseUrlViewHolder(LayoutInflater.from(context).inflate(R.layout.item_use_url, viewGroup, false))
            useUrlViewHolders.add(useUrlViewHolder)
            queryText = queryText
            return useUrlViewHolder
        }
        if (viewType == TYPE_STATUS) {
            val statusViewholder = StatusViewHolder(LayoutInflater.from(context).inflate(R.layout.item_status, viewGroup, false))
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
        when (getItemViewType(position)) {
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
        return when {
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
        val twName: TextView = view.findViewById(R.id.textViewName)
        val twURL: TextView = view.findViewById(R.id.textViewURL)
        var item: SchoolInfo? = null

        fun bind(item: SchoolInfo?) {
            this.item = item
            if (item == null) {
                clear()
            } else {
                twName.text = item.name
                twURL.text = item.url
                view.setOnClickListener { v: View? ->
                    onClicked(item)
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