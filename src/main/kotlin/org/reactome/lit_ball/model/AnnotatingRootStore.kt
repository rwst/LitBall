package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter

interface Store {
    fun refreshList()
    fun refreshClassifierButton()
}
object AnnotatingRootStore: Store {
    var state: AnnotatingRootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<RootType>

    private fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
        (RootStore::refreshList)()
    }

//    private fun RootState.updateItem(id: Int, transformer: (Paper) -> Paper): RootState =
//        copy(items = items.updateItem(id = id, transformer = transformer))

    private fun initialState(): AnnotatingRootState = AnnotatingRootState()

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }
    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query?.setting?.classifier?.isNotBlank()?: false) }
    }

    fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }
    fun onClassifierConfirmed() {
        scope.launch(Dispatchers.IO) { PaperList.applyClassifier() }
    }
    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun onEditorCloseClicked() {
        setState { copy(editingItemId = null) }
    }
    fun onFlagSet(id: Int, flagNo: Int, value: Boolean) {
        PaperList.setFlag(id, flagNo, value)
    }

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.saveAnnotated()
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
    val ydfNotFoundAlert: Boolean = false,
    val progressIndication: ProgressIndicatorParameter? = null,
    )
