package org.reactome.lit_ball.model

import RootType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.*
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.SortingControlItem
import org.reactome.lit_ball.window.components.SortingType


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

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Filled.Save, 0) { doSave() },
        RailItem("Export", "Write ${FileType.EXPORTED.fileName}", Icons.Filled.Publish, 1) { doExport() },
        RailItem("Main", "Save and go back\nto main screen", Icons.Filled.ExitToApp, 2, onClicked = { onDoAnnotateStopped() }),
        RailItem("Exit", "Exit application", Icons.Filled.ExitToApp, 3, extraAction = SystemFunction.exitApplication, onClicked = { buttonExit() })
    )
    val sortingControls: List<SortingControlItem> = listOf(
        SortingControlItem("Alphabetical sort ascending", Icons.Filled.SortByAlpha) { doSort(SortingType.ALPHA_ASCENDING) },
        SortingControlItem("Alphabetical sort descending", Icons.Filled.SortByAlpha) { doSort(SortingType.ALPHA_DESCENDING) },
    )
    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query?.setting?.classifier?.isNotBlank() ?: false) }
    }

    override fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore) {
        setState { copy(paperListStore = paperListScreenStore) }
    }

    private fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }
    fun onFlagSet(id: Int, flagNo: Int, value: Boolean) {
        PaperList.setFlag(id, flagNo, value)
    }

    private fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.saveAnnotated()
        }
        switchRoot()
    }
    private fun doExport() {
        scope?.launch(Dispatchers.IO) {
            PaperList.exportAnnotated()
        }
    }

    private fun doSave() {
        scope?.launch(Dispatchers.IO) {
            PaperList.saveAnnotated()
        }
    }
    fun doSort(sortingType: SortingType) {
        scope?.launch(Dispatchers.IO) {
            PaperList.sort(sortingType)
            refreshList()
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