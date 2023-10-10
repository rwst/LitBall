package org.reactome.lit_ball.window.components

enum class SortingType { ALPHA_ASCENDING, ALPHA_DESCENDING, NUMER_ASCENDING, NUMER_DESCENDING, DATE_ASCENDING, DATE_DESCENDING }
data class SortingControlItem(
    val tooltipText: String = "",
    val iconPainterResource: String,
    val onClicked: () -> Unit,
)