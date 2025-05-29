@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import dialog.Filtering2FilterDialog
import dialog.InformationalDialog
import model.Filtering2RootStore

@Composable
fun Filtering2RootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { Filtering2RootStore() }
    val focusRequester = remember { FocusRequester() }
    val state = model.state
    val scope = rememberCoroutineScope()
    model.rootSwitch = rootSwitch

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    state.doInformationalDialog?.let {
        InformationalDialog(title = "NOTE", it)
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
