@file:Suppress("FunctionName")

package org.reactome.lit_ball.window.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SortingControls(controls: List<SortingControlItem>) {
    Row (modifier = Modifier.fillMaxWidth()) {
        controls.forEach { item ->
            Tooltip(item.tooltipText) {
                IconButton(
                    onClick = { item.onClicked() },
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.Blue,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}