@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import org.reactome.lit_ball.dialog.Filtering2FilterDialog
import org.reactome.lit_ball.model.Filtering2RootStore
import window.RootType

@Composable
fun Filtering2RootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { Filtering2RootStore }
    val focusRequester = remember { FocusRequester() }
    val state = model.state
    val scope = rememberCoroutineScope()
    Filtering2RootStore.scope = scope
    Filtering2RootStore.state = state
    model.rootSwitch = rootSwitch

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (state.paperListStore.state.filterDialog) {
        Filtering2FilterDialog(state.paperListStore)
    }

    Filtering2MainContent(
        model = model,
        rootSwitch = rootSwitch,
        focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
