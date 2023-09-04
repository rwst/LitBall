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


object AnnotatingRootStore: ModelHandle {
    var state: AnnotatingRootState by mutableStateOf(initialState())

    var scope: CoroutineScope? = null
        set(value) {
            if (value != null) {
                Filtering2RootStore.state.paperListStore.scope = value
            }
            field = value
        }
    lateinit var rootSwitch: MutableState<RootType>

    private fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun initialState(): AnnotatingRootState = AnnotatingRootState()

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }

    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query?.setting?.classifier?.isNotBlank() ?: false) }
    }

    override fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore) {
        setState { copy(paperListStore = paperListScreenStore) }
    }

    fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
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
    fun setDoExport(doExport: Boolean) {
        if (doExport) {
            scope?.launch(Dispatchers.IO) {
                PaperList.exportAnnotated()
            }
        }
    }

    fun setDoSave(doSave: Boolean) {
        if (doSave) {
            scope?.launch(Dispatchers.IO) {
                PaperList.saveAnnotated()
            }
        }
    }
}

data class AnnotatingRootState(
    val items: List<Paper> = PaperList.toList(),
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val isClassifierSet: Boolean = false,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(AnnotatingRootStore),
)