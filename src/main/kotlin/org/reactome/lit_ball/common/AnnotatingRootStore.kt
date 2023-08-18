package org.reactome.lit_ball.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

object AnnotatingRootStore {
    var state: AnnotatingRootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<Boolean>

    fun switchRoot() {
        rootSwitch.value = false
        (RootStore::refreshList)()
    }

//    private fun RootState.updateItem(id: Int, transformer: (Paper) -> Paper): RootState =
//        copy(items = items.updateItem(id = id, transformer = transformer))

    private fun initialState(): AnnotatingRootState = AnnotatingRootState()

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }
    fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query?.setting?.classifier?.isNotBlank()?: false) }
    }

    fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }
    fun onClassifierConfirmed() {
        PaperList.applyClassifier()
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

    fun onEditorCloseClicked() {
        setState { copy(editingItemId = null) }
    }

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.save()
        }
        switchRoot()
    }

    fun setClassifierAlert(isAlertActive: Boolean) {
        setState { copy(classifierAlert = isAlertActive) }
    }

    fun setDoExport(doExport: Boolean) {
        setState { copy(doExport = doExport) }
    }

    fun setDoSave(doSave: Boolean) {
        setState { copy(doSave = doSave) }
    }

    fun setClassifierExceptionAlert(classifierExceptionAlert: Boolean) {
        setState { copy(classifierExceptionAlert = classifierExceptionAlert) }
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
    val classifierAlert: Boolean = false,
    val isClassifierSet: Boolean = false,
    val classifierExceptionAlert: Boolean = false,
    )
