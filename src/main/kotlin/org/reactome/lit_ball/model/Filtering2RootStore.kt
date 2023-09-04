package org.reactome.lit_ball.model

import RootType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.SortingControlItem

object Filtering2RootStore: ModelHandle {
    var state: Filtering2RootState by mutableStateOf(initialState())

    var scope: CoroutineScope? = null
    set(value) {
        if (value != null) {
            state.paperListStore.scope = value
        }
        field = value
    }
    lateinit var rootSwitch: MutableState<RootType>

    fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun initialState(): Filtering2RootState = Filtering2RootState()

    private inline fun setState(update: Filtering2RootState.() -> Filtering2RootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Filled.Save, 0) { doSave() },
        RailItem("Finish", "Finish filtering,\nwriting accepted/rejected", Icons.Filled.Publish, 1) { doFinish() },
        RailItem("Main", "Save and go back\nto main screen", Icons.Filled.ExitToApp, 2, onClicked = { onDoAnnotateStopped() }),
        RailItem("Exit", "Exit application", Icons.Filled.ExitToApp, 3, extraAction = SystemFunction.exitApplication, onClicked = { buttonExit() } )
    )
    val sortingControls: List<SortingControlItem> = listOf(
        SortingControlItem()
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

    fun onItemRadioButtonClicked(id: Int, btn: Int) {
        PaperList.setTag(id, btn)
        refreshList()
    }

    private fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.save()
        }
        switchRoot()
    }


    private fun doFinish() {
        scope?.launch(Dispatchers.IO) {
            PaperList.finish()
        }
    }

    private fun doSave() {
        scope?.launch(Dispatchers.IO) {
            PaperList.save()
        }
    }
}

data class Filtering2RootState (
    val items: List<Paper> = PaperList.toList(),
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val isClassifierSet: Boolean = false,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(Filtering2RootStore),
)