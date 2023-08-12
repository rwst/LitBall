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
        RailItem("Info", Icons.Filled.Info, 0, RootStore::buttonInfo),
        RailItem("Settings", Icons.Filled.Settings, 1, RootStore::buttonSettings),
        RailItem("Exit", Icons.Filled.ExitToApp, 3, RootStore::buttonExit, onExit)
    )

    MainContent(
        modifier = modifier,
        qItems = state.items,
        onItemClicked = RootStore::onItemClicked,
        railItems = railItems,
        onNewItemClicked = RootStore::onNewItemClicked,
        onItemSettingsClicked = RootStore::onQuerySettingsClicked,
        onItemGoClicked = RootStore::nextAction,
    )

    scope.launch(Dispatchers.IO) {
        Settings.load()
        QueryList.fill()
    }

    if (state.newItem) {
        NewItemDialog(
            scope,
            onCloseClicked = RootStore::onNewItemClosed,
        )
    }

    if (state.editingSettings) {
        SettingsDialog(
            scope,
            onCloseClicked = RootStore::onSettingsCloseClicked
        )
    }

    state.editingQuerySettings?.also {
        QuerySettingsDialog(
            it,
            scope,
            RootStore::onQuerySettingsCloseClicked,
        )
    }
}
