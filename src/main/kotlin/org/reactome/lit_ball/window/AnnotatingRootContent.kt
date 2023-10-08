@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.reactome.lit_ball.dialog.BarChartDialog
import org.reactome.lit_ball.model.AnnotatingRootStore

@Composable
fun AnnotatingRootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { AnnotatingRootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    AnnotatingRootStore.scope = scope
    AnnotatingRootStore.state = state // TODO: is this circular?
    model.rootSwitch = rootSwitch

    if (state.showStats) {
        BarChartDialog(model)
    }

    AnnotatingMainContent(
        model = model,
        rootSwitch = rootSwitch,
    )

    PaperListScreenEvents(state.paperListStore)
}
