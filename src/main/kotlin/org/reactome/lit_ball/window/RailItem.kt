package org.reactome.lit_ball.window

import androidx.compose.ui.graphics.vector.ImageVector

data class RailItem(
    val text: String,
    val icon: ImageVector,
    val actionIndex: Int,
    val onClicked: () -> Unit,
    val extraAction: (() -> Unit)? = null
)


