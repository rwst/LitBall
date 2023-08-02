@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.*

@Composable
fun RootContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
) {
    val model = remember { RootStore() }
    val state = model.state
    val scope = rememberCoroutineScope()

    MainContent(
        modifier = modifier,
        items = state.items,
        onExit,
        onItemClicked = model::onItemClicked,
        onItemDeleteClicked = model::onItemDeleteClicked,
        onNewItemClicked = model::onNewItemClicked,
        onRailItemClicked = model.onRailItemClicked
    )

    scope.launch(Dispatchers.IO) {
        Settings.load()
    }

    scope.launch(Dispatchers.IO) {
        SerialDB.open()
        model.setFromDb(SerialDB.get())
    }

    state.editingItemId?.also { item ->
        QueryEditDialog(
            item = state.items.list[item],
            onCloseClicked = model::onEditorCloseClicked,
            onTextChanged = model::onEditorTextChanged,
            onDoneChanged = model::onEditorDoneChanged,
        )
    }

    if (state.editingSettings) {
        SettingsDialog(
            onCloseClicked = {})
    }
}
