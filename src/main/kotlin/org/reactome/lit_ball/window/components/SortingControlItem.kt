package org.reactome.lit_ball.window.components

import androidx.compose.ui.graphics.vector.ImageVector

enum class SortingType { ALPHA_ASCENDING, ALPHA_DESCENDING, NUMER_ASCENDING, NUMER_DESCENDING, DATE_ASCENDING, DATE_DESCENDING }
data class SortingControlItem(
    val tooltipText: String = "",
    val icon: ImageVector,
    val onClicked: () -> Unit,
)