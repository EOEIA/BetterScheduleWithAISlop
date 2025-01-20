package cz.vitskalicky.lepsirozvrh.model.rozvrh

data class RozvrhBlock(
    val day: RozvrhDay,
    val caption: RozvrhCaption,
    val lessons: List<RozvrhLesson>
)