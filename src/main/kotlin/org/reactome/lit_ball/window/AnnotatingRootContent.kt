@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import org.reactome.lit_ball.dialog.AnnotatingFilterDialog
import org.reactome.lit_ball.dialog.BarChartDialog
import model.AnnotatingRootStore
import window.RootType

@Composable
fun AnnotatingRootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { AnnotatingRootStore }
    val focusRequester = remember { FocusRequester() }
    val state = model.state
    val scope = rememberCoroutineScope()
    AnnotatingRootStore.scope = scope
    AnnotatingRootStore.state = state // TODO: is this circular?
    model.rootSwitch = rootSwitch

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (state.showStats) {
        BarChartDialog(model, focusRequester)
    }

    if (state.paperListStore.state.filterDialog) {
        AnnotatingFilterDialog(state.paperListStore)
    }

    AnnotatingMainContent(
        model = model,
        rootSwitch = rootSwitch,
        focusRequester = focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
