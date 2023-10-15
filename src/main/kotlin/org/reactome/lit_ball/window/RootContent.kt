@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.reactome.lit_ball.dialog.*
import org.reactome.lit_ball.model.RootStore

@Composable
fun RootContent(
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { RootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    RootStore.scope = scope
    RootStore.state = state
    RootStore.rootSwitch = rootSwitch

    MainContent(
        model,
        rootSwitch = rootSwitch,
    )

    RootStore.init()

    if (state.newItem) {
        NewItemDialog(
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
}
