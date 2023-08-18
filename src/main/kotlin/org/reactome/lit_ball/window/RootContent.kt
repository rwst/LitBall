@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.QueryList
import org.reactome.lit_ball.common.RootStore
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.dialog.NewItemDialog
import org.reactome.lit_ball.dialog.ProgressIndicator
import org.reactome.lit_ball.dialog.QuerySettingsDialog
import org.reactome.lit_ball.dialog.SettingsDialog

@Composable
fun RootContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    rootSwitch: MutableState<Boolean>,
) {
    val model = remember { RootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    RootStore.scope = scope
    RootStore.state = state
    RootStore.rootSwitch = rootSwitch

    val railItems: List<RailItem> = listOf(
        RailItem("Info", Icons.Filled.Info, 0, onClicked = { model.buttonInfo() }),
        RailItem("Settings", Icons.Filled.Settings, 1) { model.setEditingSettings(true) },
        RailItem("Exit", Icons.Filled.ExitToApp, 3, onClicked = { model.buttonExit() }, extraAction = onExit)
    )

    MainContent(
        modifier = modifier,
        qItems = state.items,
        onItemClicked = { id -> model.setEditingItemId(id) },
        railItems = railItems,
        onNewItemClicked = { model.setNewItem(true) },
        onItemSettingsClicked = { id ->model.onQuerySettingsClicked(id) },
        onItemGoClicked = { status, id -> model.nextAction(status, id) },
        rootSwitch = rootSwitch,
    )

    scope.launch(Dispatchers.IO) {
        Settings.load()
        QueryList.fill()
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
        ProgressIndicator(
            state.progressIndication.first,
            state.progressIndication.second
        )
    }

    state.editingQuerySettings?.also {
        QuerySettingsDialog(
            it,
            scope,
        ) { model.onQuerySettingsCloseClicked() }
    }
}
