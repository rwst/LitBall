@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import org.reactome.lit_ball.model.Filtering2RootStore

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

    Filtering2MainContent(
        model = model,
        rootSwitch = rootSwitch,
        focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
