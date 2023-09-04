package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.*
import org.reactome.lit_ball.dialog.ProgressIndicatorParameter
import org.reactome.lit_ball.util.CantHappenException

object RootStore {
    var state: RootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<RootType>

    private fun initialState(): RootState =
        RootState()

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    fun buttonInfo() {
        setInformationalDialog(About.text)
    }

    fun buttonExit() {
    }

    fun refreshList() {
        setState { copy(items = QueryList.list.toList()) }
    }

    private fun onDoExpandStarted(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.expand()
            }
            copy(doExpand = id)
        }
    }

    private fun onDoFilter1Started(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.filter1()
            }
            copy(doFilter1 = id)
        }
    }

    fun setFiltered2() {
        QueryList.itemFromId(state.doFilter2)?.let {
            it.syncBuffers()
            it.status = QueryStatus.FILTERED2
        }
    }

    fun nextAction(status: QueryStatus, id: Int) {
        when (status) {
            QueryStatus.UNINITIALIZED -> onQuerySettingsClicked(id)
            QueryStatus.FILTERED2 -> onDoExpandStarted(id)
            QueryStatus.EXPANDED -> onDoFilter1Started(id)
            QueryStatus.FILTERED1 -> onDoFilter2Started(id)
        }
    }

    fun onQuerySettingsCloseClicked() {
        val query = state.editingQuerySettings ?: throw CantHappenException()
        if (query.status == QueryStatus.UNINITIALIZED) {
            query.status = QueryStatus.FILTERED2 // TODO: make this dependent on what is set
            setState { copy(items = QueryList.list.toList()) }
        }
        setState { copy(editingQuerySettings = null) }
        Filtering2RootStore.refreshClassifierButton()
    }

    fun setEditingSettings(boolean: Boolean) {
        if (!boolean)
            QueryList.fill()
        setState { copy(editingSettings = boolean, items = QueryList.list.toList()) }
    }

    fun setEditingItemId(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun setNewItem(boolean: Boolean) {
        setState { copy(newItem = boolean) }
    }

    fun onDoExpandStopped() {
        setState { copy(doExpand = null, items = QueryList.list.toList()) }
    }

    fun onDoFilter1Stopped() {
        setState { copy(doFilter1 = null, items = QueryList.list.toList()) }
    }

    private fun onDoFilter2Started(id: Int) {
        scope.launch(Dispatchers.IO) {
            state.items[id].filter2()
            PaperList.model = Filtering2RootStore.state.paperListStore
            rootSwitch.value = RootType.FILTER2_ROOT
            setState { copy(doFilter2 = id) }
        }
    }

    fun onAnnotateStarted(id: Int) {
        scope.launch(Dispatchers.IO) {
            state.items[id].annotate()
            PaperList.model = AnnotatingRootStore.state.paperListStore
            rootSwitch.value = RootType.ANNOTATE_ROOT
            setState { copy(doAnnotate = id) }
        }
    }

    fun onQuerySettingsClicked(id: Int?) {
        AnnotatingRootStore.refreshList()
        setState { copy(editingQuerySettings = QueryList.itemFromId(id)) }
    }

    fun setItems(items: List<LitBallQuery>) {
        setState { copy(items = items) }
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
}


data class RootState(
    val items: List<LitBallQuery> = QueryList.list,
    val activeRailItem: String = "",
    val newItem: Boolean = false,
    val editingItemId: Int? = null,
    val editingSettings: Boolean = false,
    val editingQuerySettings: LitBallQuery? = null, // TODO: refactor this to Int?
    val doExpand: Int? = null,
    val doFilter1: Int? = null,
    val doFilter2: Int? = null,
    val doAnnotate: Int? = null,
    val progressIndication: ProgressIndicatorParameter? = null,
    val doInformationalDialog: String? = null,
)
