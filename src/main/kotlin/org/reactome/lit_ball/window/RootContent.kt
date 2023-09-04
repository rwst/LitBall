@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.QueryList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.dialog.*
import org.reactome.lit_ball.dialog.InformationalDialog
import org.reactome.lit_ball.dialog.SettingsDialog
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.once
import org.reactome.lit_ball.window.components.RailItem

@Composable
fun RootContent(
    onExit: () -> Unit,
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { RootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    RootStore.scope = scope
    RootStore.state = state
    RootStore.rootSwitch = rootSwitch

    val railItems: List<RailItem> = listOf(
        RailItem("Info", "About LitBall", Icons.Filled.Info, 0, onClicked = { model.buttonInfo() }),
        RailItem("Settings", "General Settings", Icons.Filled.Settings, 1) { model.setEditingSettings(true) },
        RailItem("Exit", "Exit application", Icons.Filled.ExitToApp, 3, onClicked = { model.buttonExit() }, extraAction = onExit)
    )

    MainContent(
        model,
        railItems = railItems,
        rootSwitch = rootSwitch,
    )

    once {
        scope.launch(Dispatchers.IO) {
            Settings.load()
            QueryList.fill()
        }
    }

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

    state.doInformationalDialog?.also {
        InformationalDialog(title = "NOTE", text = it) { model.setInformationalDialog(null) }
    }
}
