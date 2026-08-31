package cz.vitskalicky.lepsirozvrh.model.rozvrh

import androidx.annotation.StringRes
import cz.vitskalicky.lepsirozvrh.R

@StringRes
fun LessonChangeType.labelRes(): Int = when (this) {
    LessonChangeType.NONE         -> 0
    LessonChangeType.ADDED        -> R.string.change_kind_added
    LessonChangeType.REMOVED      -> R.string.change_kind_removed
    LessonChangeType.CANCELLED    -> R.string.change_kind_cancelled
    LessonChangeType.SUBSTITUTION -> R.string.change_kind_substitution
    LessonChangeType.ROOM_CHANGED -> R.string.change_kind_room_changed
    LessonChangeType.OTHER        -> R.string.change_kind_other
}
