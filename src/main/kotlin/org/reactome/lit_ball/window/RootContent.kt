@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.*
import org.reactome.lit_ball.dialog.QueryEditDialog
import org.reactome.lit_ball.dialog.SettingsDialog

@Composable
fun RootContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
) {
    val model = remember { RootStore() }
    val state = model.state
    val scope = rememberCoroutineScope()

    val railItems: List<RailItem> = listOf(
        RailItem("Info", Icons.Filled.Info, 0, model::buttonInfo),
        RailItem("Settings", Icons.Filled.Settings, 1, model::buttonSettings),
        RailItem("Exit", Icons.Filled.ExitToApp, 3, model::buttonExit, onExit)
    )

    MainContent(
        modifier = modifier,
        qItems = state.items,
        onItemClicked = model::onItemClicked,
        onNewItemClicked = model::onNewItemClicked,
        railItems = railItems,
    )

    scope.launch(Dispatchers.IO) {
        Settings.load()
        QueryList.fill()
    }

//    scope.launch(Dispatchers.IO) {
//        SerialDB.open()
//        model.setFromDb(SerialDB.get())
//    }

    state.editingItemId?.also { item ->
//        QueryEditDialog(
//            item = state.items.list[item],
//            onCloseClicked = model::onEditorCloseClicked,
//            onTextChanged = model::onEditorTextChanged,
//            onDoneChanged = model::onEditorDoneChanged,
//        )
    }

    if (state.editingSettings) {
        SettingsDialog(
            scope,
            onCloseClicked = model::onSettingsCloseClicked
        )
    }
}
