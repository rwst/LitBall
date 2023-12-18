package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.LocalDate
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.util.toEpochMilliseconds
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.SortingControlItem
import org.reactome.lit_ball.window.components.SortingType


object AnnotatingRootStore : ModelHandle {
    var state: AnnotatingRootState by mutableStateOf(initialState())
    private var scrollChannel: Channel<Int>? = null

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
        RailItem("Stats", "Publication date statistics", Icons.BarChart, 0) { setStat(true) },
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Save, 1) { doSave() },
        RailItem("Export", "Write ${FileType.EXPORTED_CSV.fileName}", Icons.ExportNotes, 2) { doExport() },
        RailItem("Export\nText", "Write ${FileType.EXPORTED_JSONL.fileName}", Icons.ExportNotes, 2) { doExportText() },
        RailItem(
            "Main",
            "Save and go back\nto main screen",
            Icons.ArrowBack,
            2,
            onClicked = { onDoAnnotateStopped() }),
        RailItem(
            "Exit",
            "Exit application",
            Icons.Logout,
            3,
            extraAction = SystemFunction.exitApplication,
            onClicked = { buttonExit() })
    )
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

    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query.setting?.classifier?.isNotBlank() ?: false) }
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

    fun deleteClicked(id: Int) {
        PaperList.delete(id)
        refreshList()
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

    private fun doSort(sortingType: SortingType) {
        scope?.launch(Dispatchers.IO) {
            PaperList.sort(sortingType)
            refreshList()
            delay(100) // TODO: this is a hack
            scrollChannel?.send(0)
        }
    }

    fun setStat(boolean: Boolean) {
        setState { copy(showStats = boolean) }
    }

    fun getEpochs(): List<Long> = state.items
            .mapNotNull { it.details.publicationDate }
            .filter { it.isNotEmpty() }
            .map { LocalDate.parse(it).toEpochMilliseconds() }

    fun setupListScroller(theChannel: Channel<Int>) {
        scrollChannel = theChannel
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
    val showStats: Boolean = false,
)