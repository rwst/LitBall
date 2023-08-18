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

    private fun initialState(): RootState =
        RootState()

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    fun buttonInfo() {
     }

    fun buttonExit() {
    }

    fun refreshList() {
//        scope.launch { onItemsChanged() }
    }

    private fun onDoExpandStarted(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.expand()
            }
            copy(doExpand = id)
        }
    }

    private fun onDoFilterStarted(id: Int) {
        setState {
            scope.launch(Dispatchers.IO) {
                QueryList.itemFromId(id)?.filter()
            }
            copy(doFilter = id)
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

    fun onQuerySettingsCloseClicked() {
        val query = state.editingQuerySettings ?: throw CantHappenException()
        if (query.status == QueryStatus.UNINITIALIZED) {
            query.status = QueryStatus.ANNOTATED // TODO: make this dependent on what is set
            setState { copy(items = QueryList.list.toList()) }
        }
        setState { copy(editingQuerySettings = null) }
        AnnotatingRootStore.refreshClassifierButton()
    }

    fun setEditingSettings(boolean: Boolean) {
        setState { copy(editingSettings = boolean) }
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
    fun onDoFilterStopped() {
        setState { copy(doFilter = null, items = QueryList.list.toList()) }
    }

    private fun onDoAnnotateStarted(id: Int) {
        setState {
            rootSwitch.value = true
            copy(doAnnotate = id)
        }
    }
    fun onQuerySettingsClicked(id: Int?) {
        setState { copy(editingQuerySettings = QueryList.itemFromId(id)) }
    }

    fun setItems(items: List<LitBallQuery>) {
        setState { copy(items = items) }
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
