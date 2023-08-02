package org.reactome.lit_ball.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set

    fun setFromDb(map: MutableMap<String,SerialDBClass>) {
        val items = map["queries"]
        if (items is QueryList) {
            setState { copy(items = items) }
        }
        SerialDB.commit()
    }

    fun buttonInfo() {
        SerialDB.set("queries", state.items)
        SerialDB.commit()
    }
    fun buttonSettings() {
    }
    fun buttonExit() {
    }
    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun onItemDeleteClicked(id: Int) {
        setState { copy(items = QueryList(items.list.filterNot { it.id == id }.toMutableList())) }
    }

    fun onNewItemClicked() {
        setState {
            val newItem =
                Query(
                    id = items.list.maxOfOrNull(Query::id)?.plus(1) ?: 1,
                    text = "New Query"
                )

            copy(items = QueryList((items.list + newItem).toMutableList()))
        }
        SerialDB.commit()
    }

    fun onEditorCloseClicked() {
        setState { copy(editingItemId = null) }
    }

    fun onEditorTextChanged(text: String) {
        setState {
            updateItem(id = requireNotNull(editingItemId)) { it.copy(text = text) }
        }
    }

    fun onEditorDoneChanged(/* isDone: Boolean */) {
        setState {
            updateItem(id = requireNotNull(editingItemId)) { it.copy(/* isDone = isDone */) }
        }
    }

    private fun RootState.updateItem(id: Int, transformer: (Query) -> Query): RootState =
        copy(items = items.updateItem(id = id, transformer = transformer))

    private fun initialState(): RootState =
        RootState(
            items = QueryList(
                (1..5).map { id ->
                    Query(id = id, text = "Some text $id")
                }.toMutableList(),
            )
        )

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    data class RootState(
        val items: QueryList = QueryList(),
        val activeRailItem: String = "",
        val editingItemId: Int? = null,
        val editingSettings: Boolean = false,
    )
}

