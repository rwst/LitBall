package model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import common.FileType
import common.PaperList
import common.QueryStatus
import common.Settings
import common.Tag
import dialog.DialogParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import util.SystemFunction
import window.components.Icons
import window.components.RailItem
import window.RootType

class Filtering2RootStore : ModelHandle {
    var state: Filtering2RootState by mutableStateOf(initialState())
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    val modelScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): Filtering2RootState = Filtering2RootState(modelHandle = this)

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

    override fun modelScope(): CoroutineScope = modelScope

    override fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun buttonExit() {
        runBlocking {
            PaperList.saveFiltered()
        }
    }

    /**
     * Updates tags and refreshes the list.
     * This is a utility function to avoid code duplication.
     *
     * @param updateTags A function that updates the tags.
     */
    private fun updateTagsAndRefresh(updateTags: () -> Unit) {
        updateTags()
    }

    fun onItemRadioButtonClicked(id: Int, btn: Int) {
        updateTagsAndRefresh { PaperList.setTag(id, btn) }
    }

    fun acceptAll() {
        updateTagsAndRefresh { PaperList.listHandle.setFullAllTags(Tag.Accepted) }
    }

    fun rejectAll() {
        updateTagsAndRefresh { PaperList.listHandle.setFullAllTags(Tag.Rejected) }
    }
    /**
     * Launches a coroutine to perform an IO operation.
     * This is a utility function to avoid code duplication.
     *
     * @param operation A suspend function that performs the IO operation.
     */
    private fun launchIOOperation(operation: suspend () -> Unit) {
        modelScope.launch(Dispatchers.IO) {
            operation()
        }
    }

    private fun doFinish() {
        launchIOOperation {
            PaperList.finish()
            PaperList.query.let {
                it.syncBuffers()
                it.status.value = QueryStatus.FILTERED2
            }
            val nrAcc = PaperList.listHandle.getFullList().count { it.tag == Tag.Accepted }
            PaperList.query.let {
                it.noNewAccepted = (nrAcc == 0)
                it.writeNoNewAccepted()
            }
            setInformationalDialog("$nrAcc papers added to accepted") { switchRoot() }
        }
    }

    private fun doSave() {
        launchIOOperation { PaperList.saveFiltered() }
    }

    fun setInformationalDialog(text: String?, runAfter: () -> Unit = {}) {
        if (text == null)
            setState { copy(doInformationalDialog = null) }
        else
            setState { copy(doInformationalDialog = DialogParameters(text, {}, runAfter)) }
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
    val doInformationalDialog: DialogParameters? = null,
    val modelHandle: ModelHandle,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(modelHandle),
    var paperListState: PaperListScreenState = paperListStore.state
)
