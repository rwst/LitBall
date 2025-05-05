@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import dialog.AnnotatingFilterDialog
import dialog.BarChartDialog
import dialog.InformationalDialog
import dialog.ProgressIndicator
import model.AnnotatingRootStore

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

    state.progressIndication?.let {
        ProgressIndicator(it)
    }

    state.doInformationalDialog?.also {
        InformationalDialog(title = "NOTE", text = it) { model.setInformationalDialog(null) }
    }

    state.exportedNote?.let {
        InformationalDialog("NOTE", it) { model.setExportedNote(null) }
    }

    AnnotatingMainContent(
        model = model,
        rootSwitch = rootSwitch,
        focusRequester = focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
