package org.reactome.lit_ball.window.components

import androidx.compose.ui.graphics.vector.ImageVector

data class RailItem(
    val text: String,
    val icon: ImageVector,
    val actionIndex: Int,
    val extraAction: (() -> Unit)? = null,
    val onClicked: () -> Unit,
)


