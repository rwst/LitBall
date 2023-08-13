package org.reactome.lit_ball.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AnnotatingRootStore {
    var state: AnnotatingRootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<Boolean>

    fun refreshList() {
        scope.launch { onItemsChanged() }
    }
    fun buttonExport() {
        setState { copy(doExport = true) }
    }

    fun buttonSave() {
        setState { copy(doSave = true) }
    }

    fun buttonExit() {
    }

    fun onTagsButtonClicked() {
        setState { copy(editTags = true) }
    }

    fun onEnrichButtonClicked() {
        setState { copy(enrichItems = true) }
    }

    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }
    fun onItemDeleteClicked(id: Int) {
        setState { copy(items = PaperList.toListWithItemRemoved(id)) }
    }

    fun onItemRadioButtonClicked(id: Int, btn: Int) {
        PaperList.setTag(id, btn)
    }

    fun onItemFlagsClicked(active: Boolean) {
        setState { copy(itemFlags = active) }
    }

    fun onFlagSet(id: Int, flagNo: Int, value: Boolean) {
        PaperList.setFlag(id, flagNo, value)
    }

    fun onEditorCloseClicked() {
        setState { copy(editingItemId = null) }
    }

    fun onExportDoneChanged() {
        setState { copy(doExport = false) }
    }

    fun onSaveDoneChanged() {
        setState { copy(doSave = false) }
    }

    suspend fun onItemsChanged() {
        // TODO: This is a hack.
        setState { copy(items = emptyList()) }
        delay(50)
        setState { copy(items = PaperList.toList()) }
    }

    fun onDoAnnotateStopped() {
        rootSwitch.value = false
        (RootStore::refreshList)()
    }

//    private fun RootState.updateItem(id: Int, transformer: (Paper) -> Paper): RootState =
//        copy(items = items.updateItem(id = id, transformer = transformer))

    private fun initialState(): AnnotatingRootState = AnnotatingRootState()

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }
}

data class AnnotatingRootState(
    val items: List<Paper> = PaperList.toList(),
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingItemId: Int? = null,
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val doExport: Boolean = false,
    val doSave: Boolean = false,
    val editTags: Boolean = false,
    val enrichItems: Boolean = false,
    val itemFlags: Boolean = false,
)
