package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.*
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter
import org.reactome.lit_ball.util.CantHappenException
import org.reactome.lit_ball.util.DefaultScripts
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.util.openInBrowser
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.SortingControlItem
import org.reactome.lit_ball.window.components.SortingType
import java.net.URI

object RootStore {
    var state: RootState by mutableStateOf(initialState())
    private var scrollChannel: Channel<Int>? = null

    lateinit var scope: CoroutineScope
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
        RailItem("Settings", "General Settings", Icons.Settings, 2) { setEditingSettings(true) },
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
        scope.launch(Dispatchers.IO) {
            DefaultScripts.install()
            Settings.load()
            QueryList.fill()
        }
    }

    private fun buttonExit() {
        SystemFunction.exitApplication()
    }

    fun refreshList() {
        setState { copy(items = QueryList.list.toList()) }
    }

    fun refreshQueryPathDisplay() {
        val stringBuilder = StringBuilder(Settings.map["path-to-queries"])
        setState { copy(queryPath = stringBuilder.toString()) }
    }

    private fun onDoExpandStarted(id: Int) {
        scope.launch(Dispatchers.IO) {
            QueryList.itemFromId(id)?.expand()
        }
    }

    private fun onDoFilter1Started(id: Int) {
        scope.launch(Dispatchers.IO) {
            QueryList.itemFromId(id)?.filter1()
        }
    }

    fun setFiltered2() {
        QueryList.itemFromId(state.doFilter2)?.let {
            it.syncBuffers()
            it.status.value = QueryStatus.FILTERED2
        }
    }

    fun nextAction(status: QueryStatus, id: Int) {
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
            setState { copy(items = QueryList.list.toList()) }
        }
        setState { copy(editingQuerySettings = null) }
    }

    fun setEditingSettings(boolean: Boolean) {
        if (!boolean) {
            QueryList.fill()
            refreshQueryPathDisplay()
        }
        setState { copy(editingSettings = boolean, items = QueryList.list.toList()) }
    }

    fun setAboutDialog(boolean: Boolean) {
        setState { copy(aboutDialog = boolean) }
    }

    fun setEditingItemId(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun setNewItem(boolean: Boolean) {
        setState { copy(newItem = boolean) }
    }

    private fun onDoFilter2Started(id: Int) {
        scope.launch(Dispatchers.IO) {
            PaperList.model = Filtering2RootStore.state.paperListStore
            state.items[id].filter2()
            rootSwitch.value = RootType.FILTER2_ROOT
            setState { copy(doFilter2 = id) }
        }
    }

    fun onAnnotateStarted(id: Int) {
        scope.launch(Dispatchers.IO) {
            PaperList.model = AnnotatingRootStore.state.paperListStore
            state.items[id].annotate()
            rootSwitch.value = RootType.ANNOTATE_ROOT
            setState { copy(doAnnotate = id) }
        }
    }

    fun onQuerySettingsClicked(id: Int?) {
        AnnotatingRootStore.state.paperListStore.refreshList()
        setState { copy(editingQuerySettings = QueryList.itemFromId(id)) }
    }

    fun onDeleteQueryClicked(id: Int?) {
        val name = QueryList.itemFromId(id)?.name
        setState {
            copy(
                doConfirmationDialog = Pair(
                    {
                        scope.launch(Dispatchers.IO) {
                            QueryList.removeDir(id)
                            QueryList.fill()
                        }
                    },
                    "You really want to delete Query $name?"
                )
            )
        }
    }

    fun closeConfirmationDialog() {
        setState { copy(doConfirmationDialog = Pair(null, "")) }
    }

    fun onQueryPathClicked() {
        scope.launch(Dispatchers.IO) {
            QueryList.fill()
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
        } else {
            setState { copy(progressIndication = null) }
            Signal.clear()
        }
        return true
    }

    fun setInformationalDialog(text: String?) {
        setState { copy(doInformationalDialog = text) }
    }

    fun doSort(sortingType: SortingType, scrollTo: Int? = null) {
        scope.launch(Dispatchers.IO) {
            if (scrollTo == null) {
                QueryList.sort(sortingType)
                refreshList()
                delay(100) // TODO: this is a hack
                scrollChannel?.send(0)
            } else {
                val name = QueryList.list[scrollTo].name
                QueryList.sort(sortingType)
                refreshList()
                delay(100) // TODO: this is a hack
                val index = QueryList.list.indexOfFirst { it.name == name }
                if (index >= 0)
                    scrollChannel?.send(index)
                else
                    scrollChannel?.send(0)
            }
        }
    }

    fun setupListScroller(theChannel: Channel<Int>) {
        scrollChannel = theChannel
    }
}

data class RootState(
    val items: List<LitBallQuery> = QueryList.list,
    val queryPath: String? = null,
    val activeRailItem: String = "",
    val newItem: Boolean = false,
    val editingItemId: Int? = null,
    val editingSettings: Boolean = false,
    val editingQuerySettings: LitBallQuery? = null, // TODO: refactor this to Int?
    val doFilter2: Int? = null,
    val doAnnotate: Int? = null,
    val progressIndication: ProgressIndicatorParameter? = null,
    val doInformationalDialog: String? = null,
    val aboutDialog: Boolean = false,
    val doConfirmationDialog: Pair<(() -> Unit)?, String> = Pair(null, ""),
)
