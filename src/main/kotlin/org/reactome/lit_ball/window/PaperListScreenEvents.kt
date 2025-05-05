@file:Suppress("FunctionName")

package window

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import dialog.ConfirmationDialog
import dialog.InformationalDialog
import dialog.PaperDetailDialog
import model.PaperListScreenStore

@Composable
fun PaperListScreenEvents(
    model: PaperListScreenStore,
    focusRequester: FocusRequester,
) {
    val state = model.state
    state.editingItemId?.also { item ->
        PaperDetailDialog(
            item,
            model::onEditorCloseClicked,
            focusRequester,
        )
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