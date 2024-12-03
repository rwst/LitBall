@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.geom.geomHistogram
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleXDateTime
import org.jetbrains.letsPlot.skia.compose.PlotPanel
import model.AnnotatingRootStore

fun createFigure(model: AnnotatingRootStore): Figure {
    val dateEpochs = model.getEpochs()
    val data = mapOf<String, Any>("Time" to dateEpochs)

    return letsPlot(data) + geomHistogram(
        color = "dark-green",
        fill = "green",
        alpha = .3,
        size = 2.0
    ) { x = "Time" } + scaleXDateTime(format = "%Y")
}

@Composable
fun BarChartDialog(
    model: AnnotatingRootStore,
    focusRequester: FocusRequester,
) {
    androidx.compose.material.AlertDialog(
        onDismissRequest = {
            model.setStat(false)
            focusRequester.requestFocus()
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    model.setStat(false)
                    focusRequester.requestFocus()
                }
            ) {
                Text("Dismiss")
            }
        },
        title = { Text("Publications / Time Period") },
        text = {
            PlotPanel(
                figure = createFigure(model),
                modifier = Modifier.width(500.dp).height(200.dp)
            ) {}
        },
    )
}