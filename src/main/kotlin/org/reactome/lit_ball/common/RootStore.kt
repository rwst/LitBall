package org.reactome.lit_ball.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set

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
//        setState {
//            val newItem =
//                Query(
//                    id = items.list.maxOfOrNull(Query::id)?.plus(1) ?: 1,
//                    text = "New Query"
//                )
//
//            copy(items = QueryList((items.list + newItem).toMutableList()))
//        }
//        SerialDB.commit()
    }
    suspend fun onItemsChanged() {
        // TODO: This is a hack.
        setState { copy(items = emptyList()) }
        delay(50)
        setState { copy(items = QueryList.list) }
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
        val items: List<Query> = QueryList.list,
        val activeRailItem: String = "",
        val editingItemId: Int? = null,
        val editingSettings: Boolean = false,
    )
}

