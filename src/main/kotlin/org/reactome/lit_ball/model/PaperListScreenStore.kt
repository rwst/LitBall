package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import common.PaperList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import window.components.Icons
import window.components.SortingControlItem
import window.components.SortingType

interface ModelHandle {
    fun refreshClassifierButton()
    fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore)
    fun modelScope(): CoroutineScope
    fun switchRoot()
}

class PaperListScreenStore(private val handle: ModelHandle) {
    var state: PaperListScreenState by mutableStateOf(initialState())

    private fun initialState(): PaperListScreenState = PaperListScreenState()
    private var scrollChannel: Channel<Long>? = null
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
        PaperList.sort(sortingType)
    }
    fun setupListScroller(theChannel: Channel<Long>) {
        scrollChannel = theChannel
    }

    fun refreshClassifierButton() {
        handle.refreshClassifierButton()
    }

    fun onItemClicked(id: Long) {
        setState { copy(editingItemId = id) }
    }

    fun onClassifierConfirmed() {
        handle.modelScope().launch()
        {
            PaperList.applyClassifier()
        }
    }

    fun onFilterChanged(filter: String) {
        PaperList.applyFilter(filter)
        setState { copy(filterString = filter) }
    }

    fun onRemoveFiltered() {
        handle.modelScope().launch {
            PaperList.deleteFiltered()
            setFilterDialog(false)
            setState { copy(filterString = null) }
        }
    }

    fun onAcceptFiltered(value: Boolean) {
        PaperList.acceptFiltered(value)
    }

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.saveAnnotated()
        }
        setFilterDialog(false)
        handle.switchRoot()
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
    val editingItemId: Long? = null,
    val classifierAlert: Boolean = false,
    val classifierExceptionAlert: Boolean = false,
    val ydfNotFoundAlert: Boolean = false,
    val filterDialog: Boolean = false,
    val filterString: String? = null,
)