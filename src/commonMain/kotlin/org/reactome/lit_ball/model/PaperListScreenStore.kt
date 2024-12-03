package org.reactome.lit_ball.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.SortingControlItem
import org.reactome.lit_ball.window.components.SortingType
import window.RootType

interface ModelHandle {
    fun refreshClassifierButton()
    fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore)
    var scope: CoroutineScope?
    var rootSwitch: MutableState<RootType>
}

class PaperListScreenStore(private val handle: ModelHandle) {
    var state: PaperListScreenState by mutableStateOf(initialState())

    private fun initialState(): PaperListScreenState = PaperListScreenState()
    private var scrollChannel: Channel<Int>? = null
    private inline fun setState(update: PaperListScreenState.() -> PaperListScreenState) {
        state = state.update()
        handle.refreshStateFromPaperListScreenStore(this)
    }
    val sortingControls: List<SortingControlItem> = listOf(
        SortingControlItem(
            "Alphabetical sort ascending",
            Icons.SortAZ
        ) { doSort(SortingType.ALPHA_ASCENDING) },
        SortingControlItem(
            "Alphabetical sort descending",
            Icons.SortZA
        ) { doSort(SortingType.ALPHA_DESCENDING) },
        SortingControlItem(
            "Publication date sort ascending",
            Icons.Sort12
        ) { doSort(SortingType.DATE_ASCENDING) },
        SortingControlItem(
            "Publication date sort descending",
            Icons.Sort21
        ) { doSort(SortingType.DATE_DESCENDING) },
    )
    private fun doSort(sortingType: SortingType) {
        handle.scope?.launch(Dispatchers.IO) {
            PaperList.sort(sortingType)
            refreshList()
//            delay(100) // TODO: this is a hack
//            scrollChannel?.send(0)
        }
    }
    fun setupListScroller(theChannel: Channel<Int>) {
        scrollChannel = theChannel
    }

    fun refreshList() {
        setState { copy(items = PaperList.toList()) }
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

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.saveAnnotated()
        }
        setFilterDialog(false)
        switchRoot()
    }
    fun switchRoot() {
        handle.rootSwitch.value = RootType.MAIN_ROOT
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
    val items: List<Paper> = PaperList.toList(),
    val editingItemId: Int? = null,
    val classifierAlert: Boolean = false,
    val classifierExceptionAlert: Boolean = false,
    val ydfNotFoundAlert: Boolean = false,
    val progressIndication: ProgressIndicatorParameter? = null,
    val filterDialog: Boolean = false,
)