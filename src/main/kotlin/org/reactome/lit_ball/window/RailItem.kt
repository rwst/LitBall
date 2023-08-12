package org.reactome.lit_ball.window

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KFunction0

data class RailItem(
    val text: String,
    val icon: ImageVector,
    val actionIndex: Int,
    val onClicked: KFunction0<Unit>,
    val extraAction: (() -> Unit)? = null
)


