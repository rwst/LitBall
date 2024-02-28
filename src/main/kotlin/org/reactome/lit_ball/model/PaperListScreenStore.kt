package org.reactome.lit_ball.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter

interface ModelHandle {
    fun refreshList()
    fun refreshClassifierButton()
    fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore)
    var scope: CoroutineScope?
}

class PaperListScreenStore(private val handle: ModelHandle) {
    var state: PaperListScreenState by mutableStateOf(initialState())

    private fun initialState(): PaperListScreenState = PaperListScreenState()
    private inline fun setState(update: PaperListScreenState.() -> PaperListScreenState) {
        state = state.update()
        handle.refreshStateFromPaperListScreenStore(this)
    }

    fun refreshList() {
        handle.refreshList()
    }

    fun refreshClassifierButton() {
        handle.refreshClassifierButton()
    }

    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun onClassifierConfirmed() {
        handle.scope?.launch(Dispatchers.IO) { PaperList.applyClassifier() }
    }
    fun onFilterChanged(filter: String) {
        handle.scope?.launch(Dispatchers.IO) {
            PaperList.applyFilter(filter)
            refreshList()
        }
    }

    fun onRemoveFiltered() {
        handle.scope?.launch(Dispatchers.IO) {
            PaperList.deleteFiltered()
            refreshList()
        }
    }

    fun onAcceptFiltered(value: Boolean) {
        handle.scope?.launch(Dispatchers.IO) {
            PaperList.acceptFiltered(value)
            refreshList()
        }
    }

    fun onEditorCloseClicked() {
        setState { copy(editingItemId = null) }
    }

    fun setClassifierAlert(isAlertActive: Boolean) {
        setState { copy(classifierAlert = isAlertActive) }
    }

    fun setClassifierExceptionAlert(classifierExceptionAlert: Boolean) {
        setState { copy(classifierExceptionAlert = classifierExceptionAlert) }
    }

    fun setYdfNotFoundAlert(ydfNotFoundAlert: Boolean) {
        setState { copy(ydfNotFoundAlert = ydfNotFoundAlert) }
    }
    fun setFilterDialog(value: Boolean) {
        setState { copy(filterDialog = value) }
        if (!value) onFilterChanged("")
    }
}

data class PaperListScreenState(
    val editingItemId: Int? = null,
    val classifierAlert: Boolean = false,
    val classifierExceptionAlert: Boolean = false,
    val ydfNotFoundAlert: Boolean = false,
    val progressIndication: ProgressIndicatorParameter? = null,
    val filterDialog: Boolean = false,
)