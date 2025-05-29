package model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import common.FileType
import common.PaperList
import common.Settings
import dialog.DialogParameters
import dialog.ProgressIndicatorParameter
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDate
import service.getAGService
import util.SystemFunction
import util.toEpochMilliseconds
import window.RootType
import window.components.Icons
import window.components.RailItem


class AnnotatingRootStore : ModelHandle, ProgressHandler {
    var state: AnnotatingRootState by mutableStateOf(initialState())
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    val modelScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): AnnotatingRootState = AnnotatingRootState(modelHandle = this)

    private inline fun setState(update: AnnotatingRootState.() -> AnnotatingRootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
//        RailItem("Stats", "Publication date statistics", Icons.BarChart, 0) { setStat(true) },
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Save, 1) { doSave() },
        RailItem("Export\nCSV", "Write ${FileType.EXPORTED_CSV.fileName}", Icons.ExportNotes, 2) { doExport() },
        RailItem("Export\nText", "Write ${FileType.EXPORTED_JSONL.fileName}", Icons.ExportNotes, 2) { doExportText() },
        RailItem("Export\nRIS", "Write ${FileType.EXPORTED_RIS.fileName}", Icons.ExportNotes, 2) { doExportRIS() },
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

    override fun modelScope(): CoroutineScope = modelScope

    override fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun buttonExit() {
        runBlocking {
            PaperList.saveAnnotated()
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

    private suspend fun doExport() {
        getAGService().progressHandler = this
        withContext(Dispatchers.IO) {
            PaperList.exportAnnotated()
        }
        setExportedNote("Exported to ${FileType.EXPORTED_CSV.fileName}.")
    }

    private suspend fun doExportText() {
        getAGService().progressHandler = this
        withContext(Dispatchers.IO) {
            PaperList.exportText()
        }
        setExportedNote("Exported to ${FileType.EXPORTED_JSONL.fileName}.")
    }

    private suspend fun doExportRIS() {
        getAGService().progressHandler = this
        withContext(Dispatchers.IO) {
            PaperList.exportRIS()
        }
        setExportedNote("Exported to ${FileType.EXPORTED_RIS.fileName}.")
    }

    private fun doSave() {
        modelScope.launch(Dispatchers.IO) {
            PaperList.saveAnnotated()
        }
    }

    fun setStat(boolean: Boolean) {
        setState { copy(showStats = boolean) }
    }

    private object Signal {
        var signal = false
        fun set() {
            signal = true
        }

        fun clear() {
            signal = false
        }
    }

    override fun setProgressIndication(title: String, value: Float, text: String): Boolean {
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
        } else {
            setState { copy(progressIndication = null) }
            Signal.clear()
        }
        return true
    }

    fun setInformationalDialog2(text: String?, runAfter: () -> Unit = {}) {
        if (text == null)
            setState { copy(doInformationalDialog = null) }
        else
            setState { copy(doInformationalDialog = DialogParameters(text, {}, runAfter)) }
    }

    override fun setInformationalDialog(text: String?) {
        setInformationalDialog2(text) { setInformationalDialog2(null) }
    }

    fun setExportedNote(note: String?) {
        setInformationalDialog2(note) { setInformationalDialog2(null) }
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
    val progressIndication: ProgressIndicatorParameter? = null,
    val doInformationalDialog: DialogParameters? = null,
    val exportedNote: DialogParameters? = null,
    val modelHandle: ModelHandle,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(modelHandle),
    val showStats: Boolean = false,
    var paperListState: PaperListScreenState = paperListStore.state
    )