package org.reactome.lit_ball.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set

    lateinit var scope: CoroutineScope
    fun buttonInfo() {
    }

    fun buttonSettings() {
        setState { copy(editingSettings = true) }
    }

    fun buttonExit() {
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
        setState { copy(doExpand = null) }
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
        setState { copy(doFilter = null) }
    }

    private fun onDoAnnotateStarted(id: Int) {
        setState { copy(doAnnotate = id) }
    }

    fun onDoAnnotateStopped() {
        setState { copy(doAnnotate = null) }
    }

    suspend fun onItemsChanged() {
        // TODO: This is a hack.
        setState { copy(items = emptyList()) }
        delay(50)
        setState { copy(items = QueryList.list) }
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

    suspend fun onQuerySettingsCloseClicked() {
        setState { copy(items = emptyList()) }
        delay(50)
        setState { copy(editingQuerySettings = null, items = QueryList.list) }
    }

    fun onSettingsCloseClicked() {
        setState { copy(editingSettings = false) }
    }

    private fun initialState(): RootState =
        RootState()

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
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
}

