@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import dialog.*
import dialog.AboutDialog
import dialog.ConfirmationDialog
import dialog.InformationalDialog
import model.RootStore

@Composable
fun RootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { RootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    RootStore.scope = scope
    RootStore.state = state
    RootStore.rootSwitch = rootSwitch

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MainContent(
        model,
        rootSwitch = rootSwitch,
        focusRequester = focusRequester,
    )

    RootStore.init()

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

    state.doInformationalDialog?.also {
        InformationalDialog(title = "NOTE", text = it) { model.setInformationalDialog(null) }
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
