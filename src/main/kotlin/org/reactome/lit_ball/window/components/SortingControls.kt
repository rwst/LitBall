@file:Suppress("FunctionName")

package org.reactome.lit_ball.window.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SortingControls(
    controls: List<SortingControlItem>,
    focusRequester: FocusRequester? = null,
) {
    Row {
        controls.forEach { item ->
            Tooltip(item.tooltipText, Modifier.align(Alignment.CenterVertically)) {
                IconButton(
                    onClick = {
                        item.onClicked()
                        focusRequester?.requestFocus()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                ) {
                    Icon(
                        painterResource(item.iconPainterResource),
                        contentDescription = null,
                        tint = Color.Blue,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}