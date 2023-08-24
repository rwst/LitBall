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
        RailItem("Save", Icons.Filled.Save, 0) { model.setDoSave(true) },
        RailItem("Finish", Icons.Filled.Publish, 1) { model.setDoExport(true) },
        RailItem("Main", Icons.Filled.ExitToApp, 2, onClicked = model::onDoAnnotateStopped),
        RailItem("Exit", Icons.Filled.ExitToApp, 3, extraAction = onExit, onClicked = model::buttonExit)
    )

    AnnotatingMainContent(
        modifier = modifier,
        items = state.items,
        onItemClicked = model::onItemClicked,
        railItems = railItems,
        onItemDeleteClicked = model::onItemDeleteClicked,
        onItemRadioButtonClicked = model::onItemRadioButtonClicked,
        onExit,
        rootSwitch = rootSwitch,
        isClassifierSet = state.isClassifierSet,
        onClassifierButtonClicked = { model.setClassifierAlert(true) },
    )

    scope.launch(Dispatchers.IO) {
        RootStore.state.doFilter2?.let { RootStore.state.items[it].filter2() }
    }

    state.editingItemId?.also { item ->
        ItemClickedDialog(
            item,
            model::onEditorCloseClicked
        )
    }

    state.progressIndication?.also {
        ProgressIndicator(
            state.progressIndication.first,
            state.progressIndication.second
        )
    }

    if (state.doExport) {
        scope.launch(Dispatchers.IO) {
            PaperList.export()
            model.setDoExport(false)
        }
    }
    if (state.doSave) {
        scope.launch(Dispatchers.IO) {
            PaperList.save()
            model.setDoSave(false)
        }
    }
    if (state.classifierAlert) {
        ConfirmationDialog(
            title = "NOTE",
            text = "Applying the classifier will potentially change tags of all papers. Confirm?",
            onCloseClicked = { model.setClassifierAlert(false) },
            onConfirmClicked = {
                model.onClassifierConfirmed()
                model.setClassifierAlert(false)
            }
        )
    }
    if (state.classifierExceptionAlert) {
        InformationalDialog(
            title = "NOTE",
            text = "Classifier name not set in query settings, or directory in settings not found, or problems when running the classifier.\n\nPlease see the console output for details.",
            onCloseClicked = { model.setClassifierExceptionAlert(false) }
        )
    }
    if (state.ydfNotFoundAlert) {
        InformationalDialog(
            title = "NOTE",
            text = "LitBall could not run the command \"predict\".\n\nPlease make sure YDF is installed and the path to it is set in Settings.",
            onCloseClicked = { model.setYdfNotFoundAlert(false) }
        )
    }
}
