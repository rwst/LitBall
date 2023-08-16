@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.*
import org.reactome.lit_ball.dialog.*

@Composable
fun AnnotatingRootContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    rootSwitch: MutableState<Boolean>,
) {
    val model = remember { AnnotatingRootStore }
    val state = model.state
    val scope = rememberCoroutineScope()
    AnnotatingRootStore.scope = scope
    AnnotatingRootStore.state = state
    model.rootSwitch = rootSwitch

    val railItems: List<RailItem> = listOf(
        RailItem("Save", Icons.Filled.Save, 0, model::buttonSave),
        RailItem("Finish", Icons.Filled.Publish, 1, model::buttonExport),
        RailItem("Main", Icons.Filled.ExitToApp, 2, model::onDoAnnotateStopped),
        RailItem("Exit", Icons.Filled.ExitToApp, 3, model::buttonExit, onExit)
    )

    MainContent(
        modifier = modifier,
        items = state.items,
        onItemClicked = model::onItemClicked,
        railItems = railItems,
        onItemDeleteClicked = model::onItemDeleteClicked,
        onItemRadioButtonClicked = model::onItemRadioButtonClicked,
        onExit,
        onTagsButtonClicked = model::onTagsButtonClicked,
        onEnrichButtonClicked = model::onEnrichButtonClicked,
        onItemFlagsClicked = model::onItemFlagsClicked,
        onFlagSet = model::onFlagSet,
        rootSwitch = rootSwitch,
    )

    scope.launch(Dispatchers.IO) {
        RootStore.state.doAnnotate?.let { RootStore.state.items[it].annotate() }
    }

    state.editingItemId?.also { item ->
        ItemClickedDialog(
            item,
            model::onEditorCloseClicked
        )
    }

    if (state.doExport) {
        scope.launch(Dispatchers.IO) {
            PaperList.export()
            (model::onExportDoneChanged)()
        }
    }
    if (state.doSave) {
        scope.launch(Dispatchers.IO) {
            PaperList.save()
            (model::onSaveDoneChanged)()
        }
    }
}
