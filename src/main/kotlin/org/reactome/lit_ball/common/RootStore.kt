package org.reactome.lit_ball.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.util.CantHappenException

object RootStore {
    var state: RootState by mutableStateOf(initialState())

    lateinit var scope: CoroutineScope
    lateinit var rootSwitch: MutableState<Boolean>

     fun buttonInfo() {
     }

    fun buttonSettings() {
        setState { copy(editingSettings = true) }
    }

    fun buttonExit() {
    }

    fun refreshList() {
//        scope.launch { onItemsChanged() }
    }
    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun onNewItemClicked() {
        setState { copy(newItem = true) }
    }

    fun onNewItemClosed() {
        setState { copy(newItem = false) }
    }

    private fun onDoExpandStarted(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.expand()
            }
            copy(doExpand = id)
        }
    }

    fun onDoExpandStopped() {
        setState { copy(doExpand = null, items = QueryList.list.toList()) }
    }

    private fun onDoFilterStarted(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.filter()
            }
            copy(doFilter = id)
        }
    }

    fun onDoFilterStopped() {
        setState { copy(doFilter = null, items = QueryList.list.toList()) }
    }

    private fun onDoAnnotateStarted(id: Int) {
        setState {
            rootSwitch.value = true
            copy(doAnnotate = id)
        }
    }

    fun setAnnotated() {
        QueryList.itemFromId(state.doAnnotate)?.let {
            it.syncBuffers()
            it.status = QueryStatus.ANNOTATED
        }
    }

    fun nextAction(status: QueryStatus, id: Int) {
        when (status) {
            QueryStatus.UNINITIALIZED -> onQuerySettingsClicked(id)
            QueryStatus.ANNOTATED -> onDoExpandStarted(id)
            QueryStatus.EXPANDED -> onDoFilterStarted(id)
            QueryStatus.FILTERED -> onDoAnnotateStarted(id)
        }
    }

    fun onQuerySettingsClicked(id: Int?) {
        setState { copy(editingQuerySettings = QueryList.itemFromId(id)) }
    }

    fun onQuerySettingsCloseClicked() {
        val query = state.editingQuerySettings ?: throw CantHappenException()
        if (query.status == QueryStatus.UNINITIALIZED) {
            query.status = QueryStatus.ANNOTATED // TODO: make this dependent on what is set
            setState { copy(items = QueryList.list.toList()) }
        }
        setState { copy(editingQuerySettings = null) }
        AnnotatingRootStore.refreshClassifierButton()
    }

    fun onSettingsCloseClicked() {
        setState { copy(editingSettings = false) }
    }

    fun setItems(items: List<LitBallQuery>) {
        setState { copy(items = items) }
    }

    private fun initialState(): RootState =
        RootState()

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
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
    val doFilter: Int? = null,
    val doAnnotate: Int? = null,
)
