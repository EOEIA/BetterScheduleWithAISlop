package cz.vitskalicky.lepsirozvrh.view.rozvrhtable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import cz.vitskalicky.lepsirozvrh.R
import cz.vitskalicky.lepsirozvrh.model.rozvrh.LessonChangeType
import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhLesson
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.isLegible
import cz.vitskalicky.lepsirozvrh.theme.ThemeGenerator.textColorFor
import kotlin.math.max

/** Custom view for cell with lesson */
class HodinaView(context: Context?, attrs: AttributeSet?) : CellView(context, attrs) {
    private var hodina: RozvrhLesson? = null
    var event: String? = null
    private set
    /** combined width of all cells displaying an event, including padding, dividers and everything */
    var eventWidth: Int = 0
    /** how far from left does this cell start */
    var eventStart: Int = 0
    private var perm = false
    private var isTeacher = false
    private val mistPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightedDividerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homeworkPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homeworkCountPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topicPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteDotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = 0xFFFFC107.toInt() }
    /** 0 = off (existing changeType colors), 1 = colors per changeKind */
    private var changeVisualMode: Int = 0
    private var highlightWidth: Int = 0
    private var homeworkSize: Int = 0
    private var hasNote = false
    private var topHighlighted = false
    private var leftHighlighted = false
    private var cornerHighlighted = false
    private var entireHighlighted //the highlighting is thicker
            = false

    override fun getMinimumWidth(): Int {
        return if (hodina != null) {
            val hodinan: RozvrhLesson = hodina!!
            var zkrpr = hodinan.subjectAbbrev
            if (zkrpr.isEmpty()) zkrpr = hodinan.subjectName

            var zkrmist = hodinan.roomAbbrev

            var zkruc: String? = hodinan.teacherAbbrev

            if (isTeacher) {
                // to teacher's we want to show the class, not the teacher
                // the class name is saved in zkrskup and skup
                zkruc = hodinan.groups.joinToString(", ") { if (it.abbrev.isBlank()) {it.abbrev} else {it.name} }
            }
            val padding = super.getMinimumWidth()
            val primaryText = primaryTextPaint.measureText(zkrpr).toInt() + 1
            val secondaryText = (secondaryTextPaint.measureText("$zkruc ") + mistPaint.measureText(zkrmist)).toInt() + 1
            padding + max(primaryText, secondaryText)
        } else {
            super.getMinimumWidth()
        }
    }

    /**
     * Measures what the minimal width would be for an example cell with reasonably long texts. Don't forget to set theme using [setTheme] so that text size is measured correctly.
     */
    fun measureExampleWidth(): Int {
        val padding = super.getMinimumWidth()
        val primaryText = primaryTextPaint.measureText("MATH").toInt() + 1
        val secondaryText = (secondaryTextPaint.measureText("Tchr" + " ") + mistPaint.measureText("VIII.B")).toInt() + 1
        return padding + Math.max(primaryText, secondaryText)
    }

    /**
     * When the texts are packed tightly together
     */
    override fun getMinimumHeight(): Int {
        return super.getMinimumHeight() + primaryTextSize + textPadding + secondaryTextSize
    }

    /**
     * When the subject text is aligned to the center
     */
    val minimalComfortableHeight: Int
        get() = (primaryTextSize / 2 + textPadding + secondaryTextSize) * 2 + super.getMinimumHeight()

    fun hasLesson(): Boolean = hodina != null || event != null

    fun setHasNote(has: Boolean) {
        if (has == hasNote) return
        hasNote = has
        invalidate()
    }

    private var transposed = false
    fun setTransposed(transposed: Boolean) { this.transposed = transposed }

    /**
     * Updates the content to display a lesson
     */
    fun setHodina(hodina: RozvrhLesson?, perm: Boolean, isTeacher: Boolean) {
        this.hodina = hodina
        this.perm = perm
        this.isTeacher = isTeacher
        event = null
        eventStart = 0
        eventWidth = 0
        applyColors(hodina)
        invalidate()
        requestLayout()
    }

    /**
     * Updates the content to display an event. Dont forget to set [eventWidth] and [eventStart]
     * @param event title of the event, `null` to display normal empty cell
     */
    fun setEvent(event: String?){
        hodina = null
        perm = false
        this.event = event

        if (event != null) {
            //same as RozvrhLesson.CANCELLED
            backgroundPaint.color = t.cABg.toArgb()
            primaryTextPaint.color = t.cAPrimaryText.toArgb()
            secondaryTextPaint.color = t.cASecondaryText.toArgb()
            mistPaint.color = t.cARoomText.toArgb()
        }else{
            //same as hodina == null
            backgroundPaint.color = t.cEmptyBg.toArgb()
            primaryTextPaint.color = t.cHPrimaryText.toArgb()
            secondaryTextPaint.color = t.cHSecondaryText.toArgb()
            mistPaint.color = t.cHRoomText.toArgb()
        }
        invalidate()
        requestLayout()
    }

    private fun kindBgColor(kind: LessonChangeType): Color = when (kind) {
        LessonChangeType.CANCELLED, LessonChangeType.REMOVED -> t.cError
        LessonChangeType.ADDED -> t.cSecondary
        LessonChangeType.SUBSTITUTION -> t.cPrimary
        LessonChangeType.ROOM_CHANGED -> t.cHighlight
        LessonChangeType.OTHER, LessonChangeType.NONE -> t.cChngBg
    }

    private fun applyColors(hodina: RozvrhLesson?) {
        if (hodina == null) {
            backgroundPaint.color = t.cEmptyBg.toArgb()
            primaryTextPaint.color = t.cHPrimaryText.toArgb()
            secondaryTextPaint.color = t.cHSecondaryText.toArgb()
            mistPaint.color = t.cHRoomText.toArgb()
        } else if (changeVisualMode == 1 && hodina.changeKind != LessonChangeType.NONE) {
            val bg = kindBgColor(hodina.changeKind)
            val fg = textColorFor(bg)
            backgroundPaint.color = bg.toArgb()
            primaryTextPaint.color = fg.toArgb()
            secondaryTextPaint.color = fg.toArgb()
            mistPaint.color = fg.toArgb()
        } else if (hodina.changeType == RozvrhLesson.CHANGED) {
            backgroundPaint.color = t.cChngBg.toArgb()
            primaryTextPaint.color = t.cChngPrimaryText.toArgb()
            secondaryTextPaint.color = t.cChngSecondaryText.toArgb()
            mistPaint.color = t.cChngRoomText.toArgb()
        } else if (hodina.changeType == RozvrhLesson.CANCELLED) {
            backgroundPaint.color = t.cABg.toArgb()
            primaryTextPaint.color = t.cAPrimaryText.toArgb()
            secondaryTextPaint.color = t.cASecondaryText.toArgb()
            mistPaint.color = t.cARoomText.toArgb()
        } else {
            backgroundPaint.color = t.cHBg.toArgb()
            primaryTextPaint.color = t.cHPrimaryText.toArgb()
            secondaryTextPaint.color = t.cHSecondaryText.toArgb()
            mistPaint.color = t.cHRoomText.toArgb()
        }
    }

    fun setChangeVisualMode(mode: Int) {
        changeVisualMode = mode
        applyColors(hodina)
        invalidate()
    }

    fun getHodina(): RozvrhLesson? {
        return hodina
    }

    fun hightlightEdges(top: Boolean, left: Boolean, corner: Boolean) {
        topHighlighted = top
        leftHighlighted = left
        cornerHighlighted = corner
    }

    fun highlightEntire(highlight: Boolean) {
        entireHighlighted = highlight
        hightlightEdges(highlight, highlight, highlight)
    }

    override fun onDraw(canvas: Canvas) {
        // In transposed mode, draw row separators through normal lesson cells. Event rows stay visually merged.
        val drawTop = !topHighlighted && (!transposed || event == null)
        setDrawDividers(drawTop, !cornerHighlighted, !leftHighlighted && (event == null || eventStart == 0))
        super.onDraw(canvas)
        val w = width
        val h = height

        //# draw highlighted dividers
        //left
        if (leftHighlighted || entireHighlighted) {
            canvas.drawLine(dividerWidth.toFloat() / 2, dividerWidth.toFloat(), dividerWidth.toFloat() / 2, h.toFloat(), highlightedDividerPaint)
        }

        //top
        if (topHighlighted || entireHighlighted) {
            canvas.drawLine(dividerWidth.toFloat(), dividerWidth.toFloat() / 2, w.toFloat(), dividerWidth.toFloat() / 2, highlightedDividerPaint)
        }

        //corner
        if (cornerHighlighted || entireHighlighted) {
            canvas.drawPoint(dividerWidth / 2f, dividerWidth / 2f, highlightedDividerPaint)
        }

        //highlight
        if (entireHighlighted) {
            canvas.drawLine(dividerWidth.toFloat(), dividerWidth + highlightWidth / 2f, w.toFloat(), dividerWidth + highlightWidth / 2f, highlightPaint)
            canvas.drawLine(w - highlightWidth / 2f, dividerWidth + highlightWidth / 2f, w - highlightWidth / 2f, h - highlightWidth / 2f, highlightPaint)
            canvas.drawLine(w.toFloat(), h - highlightWidth / 2f, dividerWidth.toFloat(), h - highlightWidth / 2f, highlightPaint)
            canvas.drawLine(dividerWidth + highlightWidth / 2f, h - highlightWidth / 2f, dividerWidth + highlightWidth / 2f, dividerWidth + highlightWidth / 2f, highlightPaint)
        }
    }

    override fun onDrawContent(canvas: Canvas, xStart: Int, yStart: Int, xEnd: Int, yEnd: Int) {
        val h = yEnd - yStart
        val w = xEnd - xStart

        //# draw texts
        if (hodina != null) {
            val lesson: RozvrhLesson = hodina!!
            val zkrpr: String = lesson.subjectAbbrev.let { if (it.isBlank()){lesson.subjectName}else{it} }

            val zkrmist: String = lesson.roomAbbrev

            var zkruc: String = lesson.teacherAbbrev

            if (isTeacher) {
                // to teacher's we want to show the class, not the teacher
                // the class name is saved in zkrskup and skup
                zkruc = lesson.groups.joinToString(", ") { it.abbrev.ifBlank { it.name } }
            }

            var actualSecondaryTextSize: Float = if ((zkrmist + zkruc).isEmpty()) 0.0f else secondaryTextSize.toFloat()
            var actualPrimaryTextSize = primaryTextSize.toFloat()
            if (canvas.height < minimumHeight) {
                var overflow = actualPrimaryTextSize + textPadding + actualSecondaryTextSize - h
                if (overflow < 0) {
                    overflow = 0f
                }
                actualPrimaryTextSize = actualPrimaryTextSize - overflow / ((actualPrimaryTextSize + actualSecondaryTextSize) / actualPrimaryTextSize)
                if (actualSecondaryTextSize > 0) {
                    actualSecondaryTextSize = actualSecondaryTextSize - overflow / ((primaryTextSize + actualSecondaryTextSize) / actualSecondaryTextSize)
                }
            }
            primaryTextPaint.textSize = actualPrimaryTextSize
            secondaryTextPaint.textSize = actualSecondaryTextSize
            mistPaint.textSize = actualSecondaryTextSize
            var zkrprBaseline = h / 2f + actualPrimaryTextSize / 2f
            val middle = w / 2f
            var secondaryBaseline = zkrprBaseline + textPadding + actualSecondaryTextSize
            val secondaryTextWidth = secondaryTextPaint.measureText("$zkruc $zkrmist")
            val zkrucStart = middle - secondaryTextWidth / 2f
            val zkrmistStart = zkrucStart + secondaryTextPaint.measureText("$zkruc ")
            if (canvas.height < minimalComfortableHeight - (secondaryTextSize - actualSecondaryTextSize)) {
                //do not align zkrpr to center (vertically)
                //secondary text will be aligned to the bottom and zkrpr to the center of the remaining space
                secondaryBaseline = h.toFloat()
                zkrprBaseline = (secondaryBaseline - actualSecondaryTextSize) / 2 + actualPrimaryTextSize / 2f
            }

            // zkrpr
            primaryTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(zkrpr, middle + xStart, zkrprBaseline + yStart, primaryTextPaint)

            //draw secondary = teacher and room
            mistPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(zkrmist, zkrmistStart + xStart, secondaryBaseline + yStart, mistPaint)
            secondaryTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(zkruc, zkrucStart + xStart, secondaryBaseline + yStart, secondaryTextPaint)

            //draw topic (lesson theme) as a small third line if there is space
            val topic = lesson.theme
            if (topic.isNotBlank()) {
                val topicSize = secondaryTextSize * 0.75f
                val topicBaseline = secondaryBaseline + textPadding + topicSize
                if (topicBaseline + yStart <= yEnd) {
                    topicPaint.textSize = topicSize
                    topicPaint.textAlign = Paint.Align.CENTER
                    // truncate with ellipsis if needed
                    val maxW = (w - 4).toFloat()
                    val displayed = if (topicPaint.measureText(topic) > maxW) {
                        var s = topic
                        while (s.isNotEmpty() && topicPaint.measureText("$s…") > maxW) s = s.dropLast(1)
                        "$s…"
                    } else topic
                    canvas.drawText(displayed, middle + xStart, topicBaseline + yStart, topicPaint)
                }
            }

            //draw note indicator dot in bottom-right corner
            if (hasNote) {
                val noteR = (homeworkSize * 0.85f).coerceAtLeast(3f)
                canvas.drawCircle((xEnd - noteR - 2).toFloat(), (yEnd - noteR - 2).toFloat(), noteR, noteDotPaint)
            }

            //draw homework indicator: dot for 1, numbered badge for >1
            if (lesson.homeworkIds.isNotEmpty()) {
                val count = lesson.homeworkIds.size
                var use: Paint = homeworkPaint
                if (!isLegible(Color(homeworkPaint.color), Color(backgroundPaint.color), 1.5)) {
                    use = primaryTextPaint
                }
                val cx = (xEnd - homeworkSize - 2).toFloat()
                val cy = (yStart + homeworkSize + 2).toFloat()
                if (count > 1) {
                    val r = homeworkSize * 1.5f
                    canvas.drawCircle(cx, cy, r, use)
                    homeworkCountPaint.color = backgroundPaint.color
                    homeworkCountPaint.textSize = r * 1.3f
                    homeworkCountPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(count.toString(), cx, cy + r * 0.45f, homeworkCountPaint)
                } else {
                    canvas.drawCircle(cx, cy, homeworkSize.toFloat(), use)
                }
            }


            /*// draw cycle
            if (perm && hodina.getCycle() != null && !hodina.getCycle().isEmpty()){
                float cycleBaseline = zkrprBaseline - primaryTextSize - textPadding;
                secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(hodina.getCycle(), middle, cycleBaseline, secondaryTextPaint);
            }*/
        } else if (event != null){
            if (transposed) {
                // Vertical event spanning: rotate 90° and span downward across cells
                var actualPrimaryTextSize: Float = primaryTextSize.toFloat()
                if (w < actualPrimaryTextSize) actualPrimaryTextSize = w.toFloat()
                val totalH = eventWidth.toFloat()
                primaryTextPaint.textSize = actualPrimaryTextSize
                val textWidth = primaryTextPaint.measureText(event)
                if (textWidth > totalH * 0.85f) {
                    actualPrimaryTextSize *= totalH * 0.85f / textWidth
                    primaryTextPaint.textSize = actualPrimaryTextSize
                }
                val textPaddingV: Float = 20 * context.resources.displayMetrics.density
                // After canvas.rotate(90°): canvas +X = screen +Y (downward), canvas +Y = screen -X
                // canvas (tx, ty) → screen (-ty, tx)
                // tx = screen Y of text start = textPaddingV - eventStart (spans across cells)
                // ty = -(screen X center) + textSize/2 → centers text horizontally in cell
                val cx = xStart + w / 2f
                primaryTextPaint.textAlign = Paint.Align.LEFT
                canvas.save()
                canvas.rotate(90f)
                canvas.drawText(event!!, textPaddingV - eventStart + yStart, -cx + actualPrimaryTextSize / 2f, primaryTextPaint)
                canvas.restore()
            } else {
                var actualPrimaryTextSize: Float = primaryTextSize.toFloat()
                val textPaddingLeft: Float = 20 * context.resources.displayMetrics.density
                val drawableEventWidth: Float = eventWidth.toFloat() - dividerWidth - paddingLeft - textPaddingLeft - paddingRight
                if (h < actualPrimaryTextSize) actualPrimaryTextSize = h.toFloat()
                primaryTextPaint.textSize = actualPrimaryTextSize
                var textWidth: Float = primaryTextPaint.measureText(event)
                if (textWidth > drawableEventWidth){
                    val overflow: Float = textWidth - drawableEventWidth
                    actualPrimaryTextSize *= overflow / textWidth
                    primaryTextPaint.textSize = actualPrimaryTextSize
                    @Suppress("UNUSED_VALUE")
                    textWidth = primaryTextPaint.measureText(event)
                }
                primaryTextPaint.textAlign = Paint.Align.LEFT
                val xTextStart = dividerWidth + paddingLeft + textPaddingLeft
                val realXTextStart = xTextStart - eventStart
                val baseline = h / 2f + actualPrimaryTextSize / 2f
                canvas.drawText(event!!, realXTextStart, baseline, primaryTextPaint)
            }
        }
    }

    private fun addField(layout: TableLayout, resId: Int, fieldText: String?): Boolean {
        return if (fieldText != null && !fieldText.isEmpty()) {
            val tr = LayoutInflater.from(context).inflate(R.layout.lesson_details_dialog_row, null) as TableRow
            val tw1 = tr.findViewById<TextView>(R.id.textViewKey)
            val tw2 = tr.findViewById<TextView>(R.id.textViewValue)
            tw1.text = context.getString(resId)
            tw2.text = fieldText
            //tw2.setMaxLines(8000);
            //tr.addView(tw1);
            //tr.addView(tw2,new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            layout.addView(tr, TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            true
        } else {
            false
        }
    }

    /*fun showDetailDialog() {
        if (hodina == null) return
        val lesson: RozvrhLesson = hodina!!
        val builder = AlertDialog.Builder(context)
        builder.setTitle(lesson.subjectName.ifBlank { lesson.subjectAbbrev })

        val tableLayout = TableLayout(context)
        tableLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val density = context.resources.displayMetrics.density.toInt()
        tableLayout.setPadding(24 * density, 16 * density, 24 * density, 0)
        if (lesson.homeworkIds.isNotEmpty()){
            addField(tableLayout, R.string.homework, lesson.homeworkIds.size.toString())
        }
        if (perm) {
            addField(tableLayout, R.string.cycle, lesson.cycles.joinToString(", "){ it.abbrev.ifBlank { it.name }})
        }
        addField(tableLayout, R.string.group, lesson.groups.joinToString(", "){ it.abbrev.ifBlank { it.name }}) //you don't see group on the simplified tile anymore, therefore it is one of the main reasons you may want to see this dialog
        addField(tableLayout, R.string.lesson_teacher, lesson.teacherName.ifBlank { lesson.teacherAbbrev })
        addField(tableLayout, R.string.room, lesson.roomName.ifBlank { lesson.roomAbbrev })
        addField(tableLayout, R.string.subject_name, lesson.subjectName.ifBlank { lesson.subjectAbbrev })
        addField(tableLayout, R.string.topic, lesson.theme)
        addField(tableLayout, R.string.change, lesson.changeDescription)
        builder.setView(tableLayout)
        builder.setPositiveButton(R.string.close) { dialog, which -> }
        val dialog = builder.create()
        dialog.show()
    }*/

    override fun updateTheme() {
        super.updateTheme()
        mistPaint.color = t.cHRoomText.toArgb()
        mistPaint.textSize = secondaryTextSize.toFloat()
        mistPaint.typeface = Typeface.DEFAULT
        mistPaint.textAlign = Paint.Align.LEFT
        highlightPaint.color = t.cHighlight.toArgb()
        highlightWidth = dp(t.dpHighlightWidth)
        highlightPaint.strokeWidth = highlightWidth.toFloat()
        highlightedDividerPaint.color = t.cHighlight.toArgb()
        highlightedDividerPaint.strokeWidth = dividerWidth.toFloat()
        homeworkPaint.color = t.cHomework.toArgb()
        homeworkSize = dp(t.dpHomework)
        homeworkCountPaint.typeface = Typeface.DEFAULT_BOLD
        topicPaint.color = t.cHSecondaryText.toArgb()
        topicPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        topicPaint.textAlign = Paint.Align.CENTER
        applyColors(hodina)
    }

    init {
        setDrawDividers(true, true, true)
    }
}
