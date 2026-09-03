package cz.vitskalicky.lepsirozvrh.view.rozvrhtable

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.compose.ui.graphics.toArgb
import cz.vitskalicky.lepsirozvrh.model.rozvrh.Rozvrh
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme
import kotlin.math.max

class RozvrhScrollView : HorizontalScrollView {
    val rozvrhLayout: RozvrhLayout
    private var stickyDayColumn = false
    private var highlightCurrentDay = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        // Stretch the table across the viewport when it would otherwise be
        // narrower than the window (large tablets, desktop-sized windows) —
        // HorizontalScrollView only re-measures the child when its natural
        // width is *smaller* than the viewport, so on a phone, where the week
        // never fits, this changes nothing and horizontal scrolling stays.
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        rozvrhLayout = RozvrhLayout(context)
        addView(
            rozvrhLayout,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    fun setStickyDayColumn(enabled: Boolean) {
        stickyDayColumn = enabled
        rozvrhLayout.setStickyDayColumn(enabled, scrollX)
    }

    fun setHighlightCurrentDay(enabled: Boolean) {
        highlightCurrentDay = enabled
        rozvrhLayout.setHighlightCurrentDay(enabled)
    }

    fun setOnLessonPress(onLessonPress: (dayIndex: Int, captionIndex: Int, lessonInBlock: Int, lesson: RozvrhLesson) -> Unit) {
        rozvrhLayout.setOnLessonPress(onLessonPress)
    }

    fun createViews() {
        rozvrhLayout.createViews()
    }

    fun setChangeVisualMode(mode: Int) {
        rozvrhLayout.setChangeVisualMode(mode)
    }

    fun setCompact(compact: Boolean) {
        rozvrhLayout.setCompact(compact)
    }

    fun setTransposed(transposed: Boolean) {
        rozvrhLayout.setTransposed(transposed)
    }

    fun setAlternatingRows(enabled: Boolean) {
        rozvrhLayout.setAlternatingRows(enabled)
    }

    fun setAlternatingCols(enabled: Boolean) {
        rozvrhLayout.setAlternatingCols(enabled)
    }

    fun setHideEmptyHours(enabled: Boolean) {
        rozvrhLayout.setHideEmptyHours(enabled)
    }

    fun setNoteKeys(keys: Set<String>) {
        rozvrhLayout.setNoteKeys(keys)
    }

    fun setTheme(theme: RozvrhTheme) {
        setBackgroundColor(theme.cEmptyBg.toArgb())
        rozvrhLayout.setTheme(theme)
    }

    fun setRozvrh(rozvrh: Rozvrh?, isTeacher: Boolean) {
        rozvrhLayout.setRozvrh(rozvrh, isTeacher)
        rozvrhLayout.setStickyDayColumn(stickyDayColumn, scrollX)
        rozvrhLayout.setHighlightCurrentDay(highlightCurrentDay)
    }

    fun centerToCurrentLesson(screenWidth: Int, onCompleted: () -> Unit) {
        post {
            smoothScrollTo(max(0, (rozvrhLayout.currentLessonPosition() ?: 0) - screenWidth / 2), 0)
            onCompleted()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        rozvrhLayout.setStickyDayColumn(stickyDayColumn, l)
    }
}
