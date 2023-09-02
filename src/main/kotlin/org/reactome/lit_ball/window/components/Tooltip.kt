@file:Suppress("FunctionName")

package org.reactome.lit_ball.window.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            // composable tooltip content
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(255, 255, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(10.dp)
                )
            }
        },
        delayMillis = 400, // in milliseconds
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomEnd,
            offset = DpOffset(4.dp, 4.dp) // tooltip offset
        ),
    )
    { content() }
}