@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import dialog.*
import model.RootStore

@Composable
fun RootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { RootStore() }
    val state = model.state
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    model.rootSwitch = rootSwitch

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        model.init()
    }

    MainContent(
        model,
        rootSwitch = rootSwitch,
        focusRequester = focusRequester,
    )

    if (state.newItem) {
        NewQueryDialog(
            scope,
            onCloseClicked = { model.setNewItem(false) },
        )
    }

    if (state.editingSettings) {
        SettingsDialog(
            scope,
            onCloseClicked = { model.setEditingSettings(false) }
        )
    }

    state.progressIndication?.also {
        ProgressIndicator(state.progressIndication)
    }

    state.editingQuerySettings?.also {
        QuerySettingsDialog(
            it,
            scope,
        ) { model.onQuerySettingsCloseClicked() }
    }

    if (state.aboutDialog) {
        AboutDialog { model.setAboutDialog(false) }
    }

    state.doInformationalDialog?.let {
        InformationalDialog(title = "NOTE", it)
    }

    state.doPathSelector?.let {
        PathSelectorDialog(initialPath = it,
            onDismiss = { model.onQueryPathClicked(false) },
            onPathSelected = model::onQueryPathChanged,
            )
    }

    if (state.doConfirmationDialog.first != null) {
        ConfirmationDialog(
            title = "NOTE",
            text = state.doConfirmationDialog.second,
            onCloseClicked = model::closeConfirmationDialog,
            onConfirmClicked = state.doConfirmationDialog.first!!
        )
    }
}
