package model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.model.ModelHandle
import org.reactome.lit_ball.model.PaperListScreenState
import org.reactome.lit_ball.model.PaperListScreenStore
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.util.toEpochMilliseconds
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.RailItem
import window.RootType


object AnnotatingRootStore : ModelHandle {
    var state: AnnotatingRootState by mutableStateOf(initialState())

    override var scope: CoroutineScope? = null
    override lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): AnnotatingRootState = AnnotatingRootState()

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
        RailItem("Stats", "Publication date statistics", Icons.BarChart, 0) { setStat(true) },
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Save, 1) { doSave() },
        RailItem("Export", "Write ${FileType.EXPORTED_CSV.fileName}", Icons.ExportNotes, 2) { doExport() },
        RailItem("Export\nText", "Write ${FileType.EXPORTED_JSONL.fileName}", Icons.ExportNotes, 2) { doExportText() },
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
        setState { copy(paperListState = paperListScreenStore.state) }
    }

    private fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }

    fun onFlagSet(id: Int, flagNo: Int, value: Boolean) {
        PaperList.listHandle.setFlag(id, flagNo, value)
    }

    fun deleteClicked(id: Int) {
        runBlocking {
            PaperList.delete(id)
        }
        state.paperListStore.refreshList()
    }

    private fun doExport() {
        scope?.launch(Dispatchers.IO) {
            PaperList.exportAnnotated()
        }
    }

    private fun doExportText() {
        scope?.launch(Dispatchers.IO) {
            PaperList.exportText()
        }
    }

    private fun doSave() {
        scope?.launch(Dispatchers.IO) {
            PaperList.saveAnnotated()
        }
    }

    fun setStat(boolean: Boolean) {
        setState { copy(showStats = boolean) }
    }

    fun getEpochs(): List<Long> = state.paperListState.items
        .mapNotNull { it.details.publicationDate }
        .filter { it.isNotEmpty() }
        .map { LocalDate.parse(it).toEpochMilliseconds() }
}

data class AnnotatingRootState(
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val isClassifierSet: Boolean = false,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(AnnotatingRootStore),
    val showStats: Boolean = false,
    var paperListState: PaperListScreenState = paperListStore.state
    )