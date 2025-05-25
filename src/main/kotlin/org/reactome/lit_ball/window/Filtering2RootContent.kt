@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import dialog.Filtering2FilterDialog
import model.Filtering2RootStore

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
        scope = scope,
        model = model,
        rootSwitch = rootSwitch,
        focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
