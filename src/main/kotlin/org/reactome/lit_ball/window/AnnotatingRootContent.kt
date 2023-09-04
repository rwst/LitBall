@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.model.AnnotatingRootStore
import org.reactome.lit_ball.window.components.RailItem

@Composable
fun AnnotatingRootContent(
    onExit: () -> Unit,
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { AnnotatingRootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    AnnotatingRootStore.scope = scope
    AnnotatingRootStore.state = state
    model.rootSwitch = rootSwitch

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Filled.Save, 0) { model.setDoSave(true) },
        RailItem("Export", "Write ${FileType.EXPORTED.fileName}", Icons.Filled.Publish, 1) { model.setDoExport(true) },
        RailItem("Main", "Save and go back\nto main screen", Icons.Filled.ExitToApp, 2, onClicked = model::onDoAnnotateStopped),
        RailItem("Exit", "Exit application", Icons.Filled.ExitToApp, 3, extraAction = onExit, onClicked = model::buttonExit)
    )

    AnnotatingMainContent(
        items = state.items,
        onItemClicked = { state.paperListStore.onItemClicked(it) },
        railItems = railItems,
        onExit,
        rootSwitch = rootSwitch,
        isClassifierSet = state.isClassifierSet,
        onClassifierButtonClicked = { state.paperListStore.setClassifierAlert(true) },
        onFlagSet = model::onFlagSet,
    )

    PaperListScreenEvents(state.paperListStore)
}
