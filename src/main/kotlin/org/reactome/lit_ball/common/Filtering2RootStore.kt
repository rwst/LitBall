package org.reactome.lit_ball.common

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter

object Filtering2RootStore {
    var state: Filtering2RootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<RootType>

    fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
        (RootStore::refreshList)()
    }

//    private fun RootState.updateItem(id: Int, transformer: (Paper) -> Paper): RootState =
//        copy(items = items.updateItem(id = id, transformer = transformer))

    private fun initialState(): Filtering2RootState = Filtering2RootState()

    private inline fun setState(update: Filtering2RootState.() -> Filtering2RootState) {
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
        scope.launch(Dispatchers.IO) {PaperList.applyClassifier() }
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

    fun setYdfNotFoundAlert(ydfNotFoundAlert: Boolean) {
        setState { copy(ydfNotFoundAlert = ydfNotFoundAlert) }
    }

    private object Signal {
        var signal = false
        fun set() { signal = true }
        fun clear() { signal = false }
    }
    fun setProgressIndication(title: String = "", value: Float = -1f, text: String = ""): Boolean {
        if (Signal.signal) {
            setState { copy(progressIndication = null) }
            Signal.clear()
            return false
        }
        if (value >= 0) {
            setState {
                copy(progressIndication = ProgressIndicatorParameter(title, value, text) {
                    Signal.set()
                    setState { copy(progressIndication = null) }
                }
                )
            }
        }
        else {
            setState { copy(progressIndication = null) }
            Signal.clear()
        }
        return true
    }
}

data class Filtering2RootState(
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
    val ydfNotFoundAlert: Boolean = false,
    val progressIndication: ProgressIndicatorParameter? = null,
    )