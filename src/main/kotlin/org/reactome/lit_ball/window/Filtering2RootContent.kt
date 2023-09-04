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
import androidx.compose.ui.Modifier
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.model.Filtering2RootStore
import org.reactome.lit_ball.window.components.RailItem

@Composable
fun Filtering2RootContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    rootSwitch: MutableState<RootType>,
) {
    val model = remember { Filtering2RootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    Filtering2RootStore.scope = scope
    Filtering2RootStore.state = state
    model.rootSwitch = rootSwitch

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Filled.Save, 0) { model.setDoSave(true) },
        RailItem("Finish", "Finish filtering,\nwriting accepted/rejected", Icons.Filled.Publish, 1) { model.doFinish(true) },
        RailItem("Main", "Save and go back\nto main screen", Icons.Filled.ExitToApp, 2, onClicked = model::onDoAnnotateStopped),
        RailItem("Exit", "Exit application", Icons.Filled.ExitToApp, 3, extraAction = onExit, onClicked = model::buttonExit)
    )

    Filtering2MainContent(
        modifier = modifier,
        items = state.items,
        onItemClicked = { state.paperListStore.onItemClicked(it) },
        railItems = railItems,
        onItemRadioButtonClicked = model::onItemRadioButtonClicked,
        onExit,
        rootSwitch = rootSwitch,
        isClassifierSet = state.isClassifierSet,
        onClassifierButtonClicked = { state.paperListStore.setClassifierAlert(true) },
    )

    PaperListScreenEvents(state.paperListStore)
}
