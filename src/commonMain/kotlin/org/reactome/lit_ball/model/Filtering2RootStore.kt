package org.reactome.lit_ball.model

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
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.RailItem
import window.RootType

object Filtering2RootStore : ModelHandle {
    var state: Filtering2RootState by mutableStateOf(initialState())

    override var scope: CoroutineScope? = null
    override lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): Filtering2RootState = Filtering2RootState()

    private inline fun setState(update: Filtering2RootState.() -> Filtering2RootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Save, 0) { doSave() },
        RailItem("Finish", "Finish filtering,\nwriting accepted/rejected", Icons.Done, 1) { doFinish() },
        RailItem(
            "Main",
            "Save and go back\nto main screen",
            Icons.ArrowBack,
            2,
            onClicked = { state.paperListStore.onDoAnnotateStopped() }),
        RailItem(
            "Exit",
            "Exit application",
            Icons.Logout,
            3,
            extraAction = SystemFunction.exitApplication,
            onClicked = { buttonExit() })
    )

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query.setting.classifier.isNotBlank()) }
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
        state.paperListStore.refreshList()
    }

    fun acceptAll() {
        PaperList.listHandle.setFullAllTags(Tag.Accepted)
        state.paperListStore.refreshList()
    }

    fun rejectAll() {
        PaperList.listHandle.setFullAllTags(Tag.Rejected)
        state.paperListStore.refreshList()
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

data class Filtering2RootState(
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val isClassifierSet: Boolean = false,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(Filtering2RootStore),
    var paperListState: PaperListScreenState = paperListStore.state
)