package model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import common.*
import dialog.DialogParameters
import dialog.ExplodedDialogString
import dialog.MissingNotFoundDialogString
import dialog.NoNewAcceptedDialogString
import dialog.NoResultDialogString
import dialog.ProblemWritingDialogString
import dialog.ProgressIndicatorParameter
import dialog.ReceivedAcceptFinishDialogString
import dialog.ServerProblemWithMissingDialogString
import dialog.SuccessDialogString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import service.getAGService
import util.CantHappenException
import util.DefaultScripts
import util.SystemFunction
import util.openInBrowser
import window.RootType
import window.components.Icons
import window.components.RailItem
import window.components.SortingControlItem
import window.components.SortingType
import java.net.URI

class RootStore : ProgressHandler {
    var state: RootState by mutableStateOf(initialState())
    private var scrollChannel: Channel<Long>? = null
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val modelScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): RootState =
        RootState()

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
        RailItem("Info", "About LitBall", Icons.Info, 0, onClicked = { setAboutDialog(true) }),
        RailItem(
            "Doc",
            "Open documentation in browser",
            Icons.Article,
            1,
            onClicked = { openInBrowser(URI("https://litball.readthedocs.io/en/latest/")) }),
        RailItem("Settings", "General Settings", Icons.Settings, 2) {
            CoroutineScope(Dispatchers.Main).launch {
                setEditingSettings(true)
            }
        },
        RailItem("Exit", "Exit application", Icons.Logout, 3, onClicked = { buttonExit() })
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
            "Sort by last expansion\ndate, ascending",
            Icons.Sort12
        ) { doSort(SortingType.NUMER_ASCENDING) },
        SortingControlItem(
            "Sort by last expansion\ndate, descending",
            Icons.Sort21
        ) { doSort(SortingType.NUMER_DESCENDING) },
    )

    fun init() {
        if (Settings.initialized) return
        modelScope.launch(Dispatchers.IO) {
            DefaultScripts.install()
            Settings.load()
            QueryList.fill()
            doSort(SortingType.valueOf(Settings.map["query-sort-type"] ?: SortingType.ALPHA_ASCENDING.toString()))
            refreshQueryPathDisplay()
        }
        getAGService().progressHandler = this
    }

    private fun buttonExit() {
        SystemFunction.exitApplication()
    }

    private fun refreshList(itemId: Long?) {
        itemId?.let { QueryList.touchItem(itemId) }
    }

    fun refreshQueryPathDisplay() {
        val stringBuilder = StringBuilder(Settings.map["path-to-queries"])
        setState { copy(queryPath = stringBuilder.toString()) }
    }

    private fun onDoExpandStarted(id: Long) {
        val query = QueryList.itemFromId(id) ?: return
        when (query.type) {
            QueryType.EXPRESSION_SEARCH -> {
                modelScope.launch(Dispatchers.IO) {
                    val nrAcc = query.expressionSearch()
                    val dialogString = when (nrAcc) {
                        -2 -> ProblemWritingDialogString(FileType.ACCEPTED)
                        -1 -> return@launch
                         0 -> NoResultDialogString()
                        else -> ReceivedAcceptFinishDialogString(noAcc = query.nrAccepted())
                    }
                    setInformationalDialog(dialogString)
                    refreshList(id)
                }
            }
            QueryType.SNOWBALLING -> {
                modelScope.launch(Dispatchers.IO) {
                    val nrAcc = query.autoSnowBall()
                    setFiltered2()
                    rootSwitch.value = RootType.MAIN_ROOT
                    if (nrAcc > 0) {
                        setInformationalDialog(ReceivedAcceptFinishDialogString(nrAcc))
                    } else {
                        setInformationalDialog(ExplodedDialogString())
                    }
                    refreshList(query.id)
                }
            }
            QueryType.SUPERVISED_SNOWBALLING -> {
                modelScope.launch(Dispatchers.IO) {
                    val (nrNewDois, nrMissing, allNullsMissing) = query.snowBall()
                    val dialogString = when {
                        nrNewDois > EXPLODED_LIMIT -> ExplodedDialogString()
                        nrNewDois == 0 && nrMissing == 0 && !allNullsMissing -> ServerProblemWithMissingDialogString()
                        nrNewDois == 0 && nrMissing == 0 && allNullsMissing -> NoNewAcceptedDialogString()
                        nrNewDois == 0 && nrMissing != 0 && allNullsMissing -> MissingNotFoundDialogString(nrMissing)
                        nrNewDois > 0 -> SuccessDialogString(query, nrNewDois)
                        else -> "Can't happen: nrNewDois: $nrNewDois,\nnrMissing: $nrMissing, allNullsMissing: $allNullsMissing"
                    }
                    setInformationalDialog(dialogString)
                    refreshList(query.id)
                }
            }
            QueryType.SIMILARITY_SEARCH -> {
                modelScope.launch(Dispatchers.IO) {
                    val nrAcc = query.similaritySearch()
                    when (nrAcc) {
                        -1 -> setInformationalDialog(ProblemWritingDialogString(FileType.ACCEPTED))
                        -2 -> return@launch
                        else -> setInformationalDialog(ReceivedAcceptFinishDialogString(nrAcc))
                    }
                    refreshList(query.id)
                }
            }
        }
    }

    private fun onDoFilter1Started(id: Long) {
        modelScope.launch(Dispatchers.IO) {
            val (nrPaperDetails, nrRejectedDOIs) = QueryList.itemFromId(id)?.filter1() ?: return@launch
            if (nrPaperDetails == 0) return@launch
            setInformationalDialog("Retained $nrPaperDetails records\n\nrejected $nrRejectedDOIs papers, write to rejected...")
            refreshList(id)
        }
    }

    fun setFiltered2() {
        QueryList.itemFromId(state.doFilter2)?.let {
            it.syncBuffers()
            it.status.value = QueryStatus.FILTERED2
        }
    }

    fun nextAction(status: QueryStatus, id: Long) {
        getAGService().progressHandler = this
        when (status) {
            QueryStatus.UNINITIALIZED -> onQuerySettingsClicked(id)
            QueryStatus.FILTERED2 -> onDoExpandStarted(id)
            QueryStatus.EXPANDED -> onDoFilter1Started(id)
            QueryStatus.FILTERED1 -> onDoFilter2Started(id)
            QueryStatus.EXPLODED -> return
        }
    }

    fun onQuerySettingsCloseClicked() {
        val query = state.editingQuerySettings ?: throw CantHappenException()
        if (query.status.value == QueryStatus.UNINITIALIZED && query.setting.mandatoryKeyWords.isNotEmpty()
        ) {
            query.status.value = QueryStatus.FILTERED2
            refreshList(query.id)
        }
        setState { copy(editingQuerySettings = null) }
    }

    suspend fun setEditingSettings(boolean: Boolean) {
        if (!boolean) {
            QueryList.fill()
            refreshQueryPathDisplay()
            doSort(SortingType.valueOf(Settings.map["query-sort-type"] ?: SortingType.ALPHA_ASCENDING.toString()))
        }
        setState { copy(editingSettings = boolean) }
    }

    fun setAboutDialog(boolean: Boolean) {
        setState { copy(aboutDialog = boolean) }
    }

    fun setEditingItemId(id: Long) {
        setState { copy(editingItemId = id) }
    }

    fun setNewItem(boolean: Boolean) {
        if (!boolean) {
            doSort(SortingType.valueOf(Settings.map["query-sort-type"] ?: SortingType.ALPHA_ASCENDING.toString()))
        }
        setState { copy(newItem = boolean) }
    }

    private fun onDoFilter2Started(id: Long) {
        modelScope.launch(Dispatchers.IO) {
            QueryList.itemFromId(id)?.filter2()
            rootSwitch.value = RootType.FILTER2_ROOT
            setState { copy(doFilter2 = id) }
            refreshList(id)
        }
    }

    fun onAnnotateStarted(id: Long) {
        modelScope.launch(Dispatchers.IO) {
            QueryList.itemFromId(id)?.annotate()
            rootSwitch.value = RootType.ANNOTATE_ROOT
            setState { copy(doAnnotate = id) }
        }
    }

    fun onQuerySettingsClicked(id: Long?) {
        setState { copy(editingQuerySettings = QueryList.itemFromId(id)) }
    }

    fun onDeleteQueryClicked(id: Long) {
        val name = QueryList.itemFromId(id)?.name
        setState {
            copy(
                doConfirmationDialog = Pair(
                    { QueryList.remove(id) },
                    "You really want to delete Query $name?"
                )
            )
        }
    }

    fun closeConfirmationDialog() {
        setState { copy(doConfirmationDialog = Pair(null, "")) }
    }

    fun onQueryPathClicked() {
        modelScope.launch(Dispatchers.IO) {
            QueryList.fill()
            doSort(SortingType.valueOf(Settings.map["query-sort-type"] ?: SortingType.ALPHA_ASCENDING.toString()))
        }
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
        setInformationalDialog2(text = text, runAfter = { setInformationalDialog2(null) })
    }

    fun doSort(sortingType: SortingType) {
        modelScope.launch(Dispatchers.IO) {
            QueryList.sort(sortingType)
        }
    }

    fun setupListScroller(theChannel: Channel<Long>) {
        scrollChannel = theChannel
    }
}

data class RootState(
    val queryPath: String? = null,
    val activeRailItem: String = "",
    val newItem: Boolean = false,
    val editingItemId: Long? = null,
    val editingSettings: Boolean = false,
    val editingQuerySettings: LitBallQuery? = null, // TODO: refactor this to Int?
    val doFilter2: Long? = null,
    val doAnnotate: Long? = null,
    val progressIndication: ProgressIndicatorParameter? = null,
    val doInformationalDialog: DialogParameters? = null,
    val aboutDialog: Boolean = false,
    val doConfirmationDialog: Pair<(() -> Unit)?, String> = Pair(null, ""),
)