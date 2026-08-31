package cz.vitskalicky.lepsirozvrh.view.rozvrhtable

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.compose.ui.graphics.toArgb
import cz.vitskalicky.lepsirozvrh.SharedPrefs
import cz.vitskalicky.lepsirozvrh.model.rozvrh.*
import cz.vitskalicky.lepsirozvrh.theme.DefaultRozvrhThemes
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import io.sentry.Sentry
import org.joda.time.LocalDate

/** Custom layout for the schedule table */
class RozvrhLayout : ViewGroup {
    private var t: RozvrhTheme = DefaultRozvrhThemes.UNSPECIFIED
    /**
     * Creates a cell with reasonably long data and calculates its minimum width
     */
    private var naturalCellWidth = -1
        get() {
            if (field != -1 && childHeightWhenCalculatingNaturalCellWidth == childHeight) {
                return field
            }
            val view = HodinaView(context, null)
            view.setTheme(t)
            val minWidth = if (compact) {
                compactDayColumnWidth()
            } else {
                Math.max(view.measureExampleWidth(), CellView.goldenRectangle(childHeight))
            }
            field = minWidth
            childHeightWhenCalculatingNaturalCellWidth = childHeight
            return minWidth
        }
    private var childHeightWhenCalculatingNaturalCellWidth = -1
    private var rozvrh: Rozvrh? = null
    private var isTeacher = false
    private var perm = false
    private var rows = -1 //only actual lessons - add 1 to calculate with captions as well
    private var columns = -1 //only actual lessons - add 1 to calculate with day cells as well
    private var childHeight = 0
    private var cornerView: CornerView? = null
    private var denViews = ArrayList<DenView>(0)
    private var captionViews = ArrayList<CaptionView>(0)
    private var nextHodinaView: HodinaView? = null //the highlighted one
    private var nextHodinaViewRight: HodinaView? = null //the right one from the highlighted one (it has its left highlighted)
    private var nextHodinaViewBottom: HodinaView? = null //the bottom one from the highlighted one (it has its top highlighted)
    private var nextHodinaViewCorner: HodinaView? = null //the corner one from the highlighted one (it has its corner highlighted)
    private var hodinasByCaptions: Array<Array<ArrayList<HodinaView>>> = Array(0) { Array(0){ ArrayList() } } //the first paramemter is caption index, second day and the list contains all lessons in that block
    private var hodinaViewRecycler: HodinaViewRecycler =
        HodinaViewRecycler(context, t)
    private var columnSizes = IntArray(1) // includes days column
    private var stickyDayColumn = false
    private var horizontalScrollOffset = 0
    private var highlightCurrentDay = false
    private var changeVisualMode = 0
    private var compact = false
    private var transposed = false

    private var onLessonPress: (dayIndex: Int, captionIndex: Int, lessonInBlock: Int, lesson: RozvrhLesson) -> Unit = {_,_,_,_ ->}
    fun setOnLessonPress(onLessonPress: (dayIndex: Int, captionIndex: Int, lessonInBlock: Int, lesson: RozvrhLesson) -> Unit){
        this.onLessonPress = onLessonPress
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun setStickyDayColumn(enabled: Boolean, scrollOffset: Int) {
        val sanitizedScrollOffset = Math.max(0, scrollOffset)
        val layoutChanged = stickyDayColumn != enabled
        val previousScrollOffset = horizontalScrollOffset
        val scrollChanged = previousScrollOffset != sanitizedScrollOffset
        stickyDayColumn = enabled
        horizontalScrollOffset = sanitizedScrollOffset
        if (!transposed) {
            denViews.forEach { it.setCompact(enabled) }
        }
        val leftColumnSticky = enabled || transposed
        if (layoutChanged) {
            requestLayout()
        } else if (scrollChanged && leftColumnSticky) {
            val delta = sanitizedScrollOffset - previousScrollOffset
            cornerView?.offsetLeftAndRight(delta)
            if (!transposed) {
                denViews.forEach { it.offsetLeftAndRight(delta) }
            } else {
                captionViews.forEach { it.offsetLeftAndRight(delta) }
            }
            invalidate()
        }
    }

    fun setHighlightCurrentDay(enabled: Boolean) {
        if (highlightCurrentDay == enabled) {
            return
        }
        highlightCurrentDay = enabled
        updateCurrentDayHighlight()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specWS = MeasureSpec.getSize(widthMeasureSpec)
        val specWM = MeasureSpec.getMode(widthMeasureSpec)
        val specHS = MeasureSpec.getSize(heightMeasureSpec)
        @Suppress("UNUSED_VARIABLE")
        val specHM = MeasureSpec.getMode(heightMeasureSpec)
        val width: Int;
        val height = specHS
        var childState = 0
        childHeight = Math.ceil(specHS.toDouble() / (rows + 1)).toInt()
        val naturalCellWidth = naturalCellWidth

        //calculate width of every column
        if (!transposed) {
            columnSizes[0] = if (stickyDayColumn) compactDayColumnWidth() else naturalCellWidth
            for (i in denViews.indices) {
                columnSizes[0] = Math.max(columnSizes[0], denViews[i].minimumWidth)
            }
        } else {
            columnSizes[0] = naturalCellWidth
            for (i in captionViews.indices) {
                columnSizes[0] = Math.max(columnSizes[0], captionViews[i].minimumWidth)
            }
        }
        for (i in 1 until columnSizes.size) {
            columnSizes[i] = if (!transposed) {
                Math.max(naturalCellWidth, captionViews[i - 1].minimumWidth)
            } else {
                Math.max(naturalCellWidth, denViews[i - 1].minimumWidth)
            }
            for (j in 0 until rows) {
                var max = 0
                var count = 0
                for (item in hodinasByCaptions[i - 1][j]) {
                    max = Math.max(max, item.minimumWidth)
                    count++
                }
                columnSizes[i] = Math.max(columnSizes[i], max * count)
            }
        }
        var prefferedWidth = 0
        for (columnSize in columnSizes) {
            prefferedWidth += columnSize
        }
        if (specWM == MeasureSpec.UNSPECIFIED || specWM == MeasureSpec.AT_MOST && prefferedWidth <= specWS) {
            width = prefferedWidth
        } else {
            val widthRatio = specWS / prefferedWidth.toFloat()
            for (i in columnSizes.indices) {
                columnSizes[i] = Math.floor((columnSizes[i] * widthRatio).toDouble()).toInt()
            }
            width = specWS
        }
        if (rows == 0 || columns == 0) {
            setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState),
                    resolveSizeAndState(specHS, heightMeasureSpec,
                            childState shl MEASURED_HEIGHT_STATE_SHIFT))
            return
        }
        val childHeightMS = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
        cornerView?.let{
            measureChild(it, MeasureSpec.makeMeasureSpec(columnSizes[0], MeasureSpec.EXACTLY), childHeightMS)
            childState = combineMeasuredStates(childState, it.measuredState)
        }
        if (!transposed) {
            for (item in denViews) {
                measureChild(item, MeasureSpec.makeMeasureSpec(columnSizes[0], MeasureSpec.EXACTLY), childHeightMS)
                childState = combineMeasuredStates(childState, item.measuredState)
            }
            for (i in captionViews.indices) {
                val item = captionViews[i]
                val hodinaWidthMS = MeasureSpec.makeMeasureSpec(columnSizes[i + 1], MeasureSpec.EXACTLY)
                measureChild(item, hodinaWidthMS, childHeightMS)
                childState = combineMeasuredStates(childState, item.measuredState)
            }
        } else {
            for (item in captionViews) {
                measureChild(item, MeasureSpec.makeMeasureSpec(columnSizes[0], MeasureSpec.EXACTLY), childHeightMS)
                childState = combineMeasuredStates(childState, item.measuredState)
            }
            for (i in denViews.indices) {
                val item = denViews[i]
                val hodinaWidthMS = MeasureSpec.makeMeasureSpec(columnSizes[i + 1], MeasureSpec.EXACTLY)
                measureChild(item, hodinaWidthMS, childHeightMS)
                childState = combineMeasuredStates(childState, item.measuredState)
            }
        }
        var spaceToLeft: Int = 0 //how much space is there to the left from the cell
        var allHodinaCellsWidth = width - columnSizes[0]
        for (i in hodinasByCaptions.indices) {
            for (j in 0 until hodinasByCaptions[i].size) {
                for (item in hodinasByCaptions[i][j]) {
                    val hodinaWidthMS = MeasureSpec.makeMeasureSpec((columnSizes[i + 1] / hodinasByCaptions[i][j].size), MeasureSpec.EXACTLY)
                    measureChild(item, hodinaWidthMS, childHeightMS)
                    childState = combineMeasuredStates(childState, item.measuredState)
                    if (item.event != null){
                        item.eventWidth = allHodinaCellsWidth
                        item.eventStart = spaceToLeft
                    }
                }
            }
            spaceToLeft += columnSizes[i + 1]
        }
        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState),
                resolveSizeAndState(height, heightMeasureSpec,
                        childState shl MEASURED_HEIGHT_STATE_SHIFT))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (rows == 0 || columns == 0) {
            return
        }
        val leftColumnSticky = stickyDayColumn || transposed
        val leftColumnLeft = if (leftColumnSticky) l + horizontalScrollOffset else l
        cornerView?.layout(leftColumnLeft, t, leftColumnLeft + columnSizes[0], t + childHeight)
        if (!transposed) {
            for (i in denViews.indices) {
                denViews[i].layout(leftColumnLeft, t + (i + 1) * childHeight, leftColumnLeft + columnSizes[0], t + (i + 2) * childHeight)
            }
            var prevColumnEnd = l + columnSizes[0]
            for (i in captionViews.indices) {
                val thisColumnEnd = prevColumnEnd + columnSizes[i + 1]
                if (i == columns - 1) {
                    captionViews[i].layout(prevColumnEnd, t, r, t + childHeight)
                } else {
                    captionViews[i].layout(prevColumnEnd, t, thisColumnEnd, t + childHeight)
                }
                prevColumnEnd = thisColumnEnd
            }
        } else {
            for (i in captionViews.indices) {
                captionViews[i].layout(leftColumnLeft, t + (i + 1) * childHeight, leftColumnLeft + columnSizes[0], t + (i + 2) * childHeight)
            }
            var prevColumnEnd = l + columnSizes[0]
            for (i in denViews.indices) {
                val thisColumnEnd = prevColumnEnd + columnSizes[i + 1]
                if (i == columns - 1) {
                    denViews[i].layout(prevColumnEnd, t, r, t + childHeight)
                } else {
                    denViews[i].layout(prevColumnEnd, t, thisColumnEnd, t + childHeight)
                }
                prevColumnEnd = thisColumnEnd
            }
        }
        var prevColumnEnd = l + columnSizes[0]
        for (i in 0 until columns) {
            val thisColumnEnd = prevColumnEnd + columnSizes[i + 1]
            for (j in 0 until rows) {
                val cellWidth: Int = if (hodinasByCaptions[i][j].size > 0) {
                    columnSizes[i + 1] / hodinasByCaptions[i][j].size
                } else {
                    columnSizes[i + 1]
                }
                var lastCellEnd = prevColumnEnd
                val views: List<HodinaView> = hodinasByCaptions[i][j]
                for (k in views.indices) {
                    val item = views[k]
                    if (i == columns - 1 && k == views.size - 1) {
                        //the last one
                        item.layout(lastCellEnd, t + childHeight + j * childHeight, r, t + childHeight + (j + 1) * childHeight)
                    } else {
                        item.layout(lastCellEnd, t + childHeight + j * childHeight, lastCellEnd + cellWidth, t + childHeight + (j + 1) * childHeight)
                    }
                    lastCellEnd += cellWidth
                }
            }
            prevColumnEnd = thisColumnEnd
        }
        if (leftColumnSticky) {
            cornerView?.bringToFront()
            if (!transposed) {
                denViews.forEach { it.bringToFront() }
            } else {
                captionViews.forEach { it.bringToFront() }
            }
        }
    }

    fun createViews() {
        //debug timing: Log.d(TAG_TIMER, "createViews start " + Utils.getDebugTime());
        if (rows == -1 && columns == -1) {
            rows = getRememberedRows()
            columns = getRememberedColumns()
        }
        for (i in hodinasByCaptions.indices) {
            for (j in hodinasByCaptions[i].indices) {
                for (item in hodinasByCaptions[i][j]) {
                    removeView(item)
                    hodinaViewRecycler.store(item)
                }
                hodinasByCaptions[i][j].clear()
            }
        }
        val expectedDenCount = if (transposed) columns else rows
        val expectedCapCount = if (transposed) rows else columns
        if (denViews.size == expectedDenCount && captionViews.size == expectedCapCount && hodinasByCaptions.size == columns && (hodinasByCaptions.size == 0 || hodinasByCaptions[0].size == rows) && cornerView != null) {
            //debug timing: Log.d(TAG_TIMER, "createViews end " + Utils.getDebugTime());
            return
        }
        removeAllViews()
        denViews = ArrayList()
        captionViews = ArrayList()
        hodinasByCaptions = Array<Array<ArrayList<HodinaView>>>(columns) { Array(rows){ ArrayList() } }
        columnSizes = IntArray(columns + 1)

        if (cornerView == null) {
            cornerView = CornerView(context, null)
            cornerView!!.setTheme(t)
        }
        addView(cornerView)
        for (i in 0 until expectedCapCount) {
            val item = CaptionView(context, null)
            item.setTheme(t)
            item.setTransposed(transposed)
            captionViews.add(item)
            addView(item)
        }
        for (i in 0 until expectedDenCount) {
            val denCell = DenView(context, null)
            denCell.setTheme(t)
            denCell.setCompact(stickyDayColumn && !transposed)
            denCell.setRowHighlighted(false)
            denViews.add(denCell)
            addView(denCell)
        }

        //debug timing: Log.d(TAG_TIMER, "createViews end " + Utils.getDebugTime());
    }


    fun getRememberedRows(): Int {
        return if (!SharedPrefs.contains(context, SharedPrefs.REMEMBERED_ROWS)) 0 else SharedPrefs.getInt(context, SharedPrefs.REMEMBERED_ROWS)
    }

    fun getRememberedColumns(): Int {
        return if (!SharedPrefs.contains(context, SharedPrefs.REMEMBERED_COLUMNS)) 0 else SharedPrefs.getInt(context, SharedPrefs.REMEMBERED_COLUMNS)
    }

    fun rememberRows( rows: Int) {
        SharedPrefs.setInt(context, SharedPrefs.REMEMBERED_ROWS, rows)
    }

    fun rememberColumns( columns: Int) {
        SharedPrefs.setInt(context, SharedPrefs.REMEMBERED_COLUMNS, columns)
    }

    fun setRozvrh(rozvrh: Rozvrh?, isTeacher: Boolean) {
        //debug timing: Log.d(TAG_TIMER, "populate start " + Utils.getDebugTime());
        //todo sentry extra
        /*if (rozvrh != null) {
            Sentry.getContext().addExtra("rozvrh", oldRozvrh.getStructure())
            Log.d(TAG, """
     Rozvrh structure:
     ${oldRozvrh.getStructure()}
     """.trimIndent())
        } else {
            Sentry.getContext().addExtra("rozvrh", "null")
            Log.d(TAG, """
     Rozvrh structure:
     null
     """.trimIndent())
        }*/
        this.rozvrh = rozvrh
        this.isTeacher = isTeacher
        if (rozvrh == null) {
            empty()
            return
        }
        if (!transposed) {
            rows = rozvrh.days.size
            columns = rozvrh.captions.size
        } else {
            rows = rozvrh.captions.size
            columns = rozvrh.days.size
        }
        perm = rozvrh.permanent
        createViews()
        rememberRows(rows)
        rememberColumns(columns)

        //populate
        cornerView?.text = rozvrh.cycle?.name ?: ""
        if (!transposed) {
            for (i in 0 until columns) {
                captionViews[i].caption = rozvrh.captions[i]
            }
            for (i in 0 until rows) {
                val den: RozvrhDay = rozvrh.days[i]
                denViews[i].rozvrhDay = den

                if (den.event == null){
                    den.blocks.forEachIndexed {index: Int, it ->
                        it.forEach {
                            val view = hodinaViewRecycler.retrieve()
                            view.setChangeVisualMode(changeVisualMode)
                            view.setHodina(it, perm, isTeacher)
                            view.setRowHighlighted(false)
                            addView(view)
                            hodinasByCaptions[index][i].add(view)
                            view.setOnClickListener {_ ->
                                onLessonPress(i,index,hodinasByCaptions[index][i].size - 1, it)
                            }
                        }
                        it.ifEmpty {
                            val view = hodinaViewRecycler.retrieve()
                            view.setChangeVisualMode(changeVisualMode)
                            view.setHodina(null, perm, isTeacher)
                            view.setRowHighlighted(false)
                            addView(view)
                            hodinasByCaptions[index][i].add(view)
                        }
                    }
                }else{
                    for (j in 0 until columns){
                        val view = hodinaViewRecycler.retrieve()
                        view.setEvent(den.event)
                        view.setRowHighlighted(false)
                        addView(view)
                        hodinasByCaptions[j][i].add(view)
                    }
                }
            }
        } else {
            // transposed: captionViews in left column, denViews in top row
            // hodinasByCaptions[dayIndex][captionIndex]
            for (i in 0 until rows) {
                captionViews[i].caption = rozvrh.captions[i]
            }
            for (dayIndex in 0 until columns) {
                val den: RozvrhDay = rozvrh.days[dayIndex]
                denViews[dayIndex].rozvrhDay = den

                if (den.event == null) {
                    den.blocks.forEachIndexed { captionIndex, block ->
                        block.forEach { lesson ->
                            val view = hodinaViewRecycler.retrieve()
                            view.setChangeVisualMode(changeVisualMode)
                            view.setHodina(lesson, perm, isTeacher)
                            view.setRowHighlighted(false)
                            addView(view)
                            hodinasByCaptions[dayIndex][captionIndex].add(view)
                            view.setOnClickListener { _ ->
                                onLessonPress(dayIndex, captionIndex, hodinasByCaptions[dayIndex][captionIndex].size - 1, lesson)
                            }
                        }
                        block.ifEmpty {
                            val view = hodinaViewRecycler.retrieve()
                            view.setChangeVisualMode(changeVisualMode)
                            view.setHodina(null, perm, isTeacher)
                            view.setRowHighlighted(false)
                            addView(view)
                            hodinasByCaptions[dayIndex][captionIndex].add(view)
                        }
                    }
                } else {
                    for (captionIndex in 0 until rows) {
                        val view = hodinaViewRecycler.retrieve()
                        view.setEvent(den.event)
                        view.setRowHighlighted(false)
                        addView(view)
                        hodinasByCaptions[dayIndex][captionIndex].add(view)
                    }
                }
            }
        }
        updateCurrentDayHighlight()
        highlightCurrentLesson()
        invalidate()
        requestLayout()

        //debug timing: Log.d(TAG_TIMER, "populate end " + Utils.getDebugTime());
    }

    fun highlightCurrentLesson() {
        val indexes = rozvrh?.getHighlightBlockIndexes(false)
        val dayIndex = indexes?.first
        val captionIndex = indexes?.second
        val toHighlight: RozvrhBlock? = if (dayIndex == null || captionIndex == null)
            null
        else rozvrh?.getAsRozvrhBlock(dayIndex, captionIndex);


        //unhighlight
        nextHodinaView?.hightlightEdges(false, false, false)
        nextHodinaView?.highlightEntire(false)
        nextHodinaViewRight?.hightlightEdges(false, false, false)
        nextHodinaViewBottom?.hightlightEdges(false, false, false)
        nextHodinaViewCorner?.hightlightEdges(false, false, false)

        nextHodinaView = null
        nextHodinaViewRight = null
        nextHodinaViewBottom = null
        nextHodinaViewCorner = null
        if (toHighlight == null || dayIndex == null || captionIndex == null) {
            return
        }

        //fail-safe
        if (captionIndex >= hodinasByCaptions.size || dayIndex >= hodinasByCaptions[captionIndex].size) {
            // the rozvrh is very weird
            val msg = "There are more lessons than captions in a weekly schedule."
            Log.w(TAG, msg)
            Sentry.addBreadcrumb(msg)
            // todo: allow user to submit the weird schedule
        }

        nextHodinaView = if (!transposed) {
            hodinasByCaptions[captionIndex][dayIndex].firstOrNull()
        } else {
            hodinasByCaptions[dayIndex][captionIndex].firstOrNull()
        } ?: return //todo report error. this should not happen

        nextHodinaView?.hightlightEdges(true, true, true)
        nextHodinaView?.highlightEntire(true)
        if (!transposed) {
            if (dayIndex + 1 < rows && captionIndex < columns) {
                nextHodinaViewBottom = hodinasByCaptions[captionIndex][dayIndex + 1].firstOrNull()
                nextHodinaViewBottom?.hightlightEdges(true, false, true)
            }
            if (dayIndex < rows && captionIndex + 1 < columns) {
                nextHodinaViewRight = hodinasByCaptions[captionIndex + 1][dayIndex].firstOrNull()
                nextHodinaViewRight?.hightlightEdges(false, true, true)
            }
            if (dayIndex + 1 < rows && captionIndex + 1 < columns) {
                nextHodinaViewCorner = hodinasByCaptions[captionIndex + 1][dayIndex + 1].firstOrNull()
                nextHodinaViewCorner?.hightlightEdges(false, false, true)
            }
        } else {
            // transposed: columns=days, rows=captions; Right=next day, Bottom=next caption
            if (dayIndex + 1 < columns && captionIndex < rows) {
                nextHodinaViewRight = hodinasByCaptions[dayIndex + 1][captionIndex].firstOrNull()
                nextHodinaViewRight?.hightlightEdges(false, true, true)
            }
            if (dayIndex < columns && captionIndex + 1 < rows) {
                nextHodinaViewBottom = hodinasByCaptions[dayIndex][captionIndex + 1].firstOrNull()
                nextHodinaViewBottom?.hightlightEdges(true, false, true)
            }
            if (dayIndex + 1 < columns && captionIndex + 1 < rows) {
                nextHodinaViewCorner = hodinasByCaptions[dayIndex + 1][captionIndex + 1].firstOrNull()
                nextHodinaViewCorner?.hightlightEdges(false, false, true)
            }
        }
    }

    // we want to center when: the user opens the app, user taps current week
    // we don't want to center when: a fresh schedule with minor changes loads, user switches to the schedule using arrows.
    fun currentLessonPosition(): Int? {
        if (nextHodinaView == null) return null
        return nextHodinaView!!.x.toInt() + nextHodinaView!!.width / 2
    }

    /**
     * Empty the table when loading to prevent confusion
     */
    fun empty() {
        rozvrh = null

        //clear
        cornerView?.text = ""
        captionViews.forEach { it.caption = null }
        denViews.forEach { it.rozvrhDay = null }
        for (i in 0 until columns) {
            for (j in 0 until rows) {
                if (hodinasByCaptions[i][j].size == 0) {
                    val view = hodinaViewRecycler.retrieve()
                    addView(view)
                    hodinasByCaptions[i][j].add(view)
                }
                hodinasByCaptions[i][j].forEach { it.setHodina(null, perm, false) }
            }
        }
        highlightCurrentLesson()
        updateCurrentDayHighlight()
        invalidate()
        requestLayout()
        //debug timing: Log.d(TAG_TIMER, "populate end " + Utils.getDebugTime());
    }

    fun setChangeVisualMode(mode: Int) {
        changeVisualMode = mode
        hodinasByCaptions.forEach { it.forEach { it.forEach { it.setChangeVisualMode(mode) } } }
    }

    fun setCompact(compact: Boolean) {
        this.compact = compact
        naturalCellWidth = -1
        requestLayout()
    }

    fun setTheme(theme: RozvrhTheme){
        this.t = theme

        setBackgroundColor(theme.cEmptyBg.toArgb())
        cornerView?.setTheme(t)
        captionViews.forEach{it.setTheme(t)}
        denViews.forEach{it.setTheme(t)}
        hodinaViewRecycler.theme = t
        hodinasByCaptions.forEach { it.forEach { it.forEach { it.setTheme(t) } } }
        naturalCellWidth = -1
        requestLayout()
    }

    private fun updateCurrentDayHighlight() {
        val currentRozvrh = rozvrh
        val today = LocalDate.now()
        val currentDayIndex = if (highlightCurrentDay && currentRozvrh != null) {
            currentRozvrh.days.indexOfFirst {
                if (currentRozvrh.permanent) it.date.dayOfWeek == today.dayOfWeek else it.date == today
            }
        } else {
            -1
        }

        denViews.forEachIndexed { index, denView ->
            denView.setRowHighlighted(index == currentDayIndex)
        }
        if (!transposed) {
            hodinasByCaptions.forEach { rowsByCaption ->
                rowsByCaption.forEachIndexed { index, views ->
                    views.forEach { it.setRowHighlighted(index == currentDayIndex) }
                }
            }
        } else {
            // hodinasByCaptions[dayIndex][captionIndex] — highlight the current day's column
            hodinasByCaptions.forEachIndexed { dayIdx, captionRows ->
                captionRows.forEach { views ->
                    views.forEach { it.setRowHighlighted(dayIdx == currentDayIndex) }
                }
            }
        }
    }

    fun setTransposed(transposed: Boolean) {
        if (this.transposed == transposed) return
        this.transposed = transposed
        rows = -1
        columns = -1
        naturalCellWidth = -1
        val currentRozvrh = rozvrh
        if (currentRozvrh != null) {
            setRozvrh(currentRozvrh, isTeacher)
        } else {
            createViews()
            requestLayout()
        }
    }

    private fun compactDayColumnWidth(): Int {
        return Math.ceil(48 * resources.displayMetrics.density.toDouble()).toInt()
    }

    companion object {
        val TAG = RozvrhLayout::class.java.simpleName
    }
}
