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
    val model = remember { AnnotatingRootStore() }
    val focusRequester = remember { FocusRequester() }
    val state = model.state
    val scope = rememberCoroutineScope()
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

    state.doInformationalDialog?.let {
        InformationalDialog(title = "NOTE", it)
    }

    AnnotatingMainContent(
        scope = scope,
        model = model,
        rootSwitch = rootSwitch,
        focusRequester = focusRequester,
    )

    PaperListScreenEvents(state.paperListStore, focusRequester)
}
