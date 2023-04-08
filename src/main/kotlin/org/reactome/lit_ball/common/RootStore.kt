package org.reactome.lit_ball.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class RootStore {
    var state: RootState by mutableStateOf(initialState())
        private set
    var map: MutableMap<String,SerialDBClass> = mutableMapOf()
        set(value) {
            field = value
            val items = value["queries"]
            items.tryCast<List<Query>> {
                setState { copy(items = this@tryCast) }
            }
            val settings = value["settings"]
            if (settings is Settings) {
                setState { copy(settings = settings) }
            }
        }

    val onRailItemClicked: List<() -> Unit> = listOf(::buttonInfo, ::buttonSettings, ::buttonExit)
    private fun buttonInfo() {
    }
    private fun buttonSettings() {
    }
    private fun buttonExit() {
    }
    fun onItemClicked(id: Int) {
        setState { copy(editingItemId = id) }
    }

    fun onItemDeleteClicked(id: Int) {
        setState { copy(items = items.filterNot { it.id == id }) }
    }

    fun onNewItemClicked() {
        setState {
            val newItem =
                Query(
                    id = items.maxOfOrNull(Query::id)?.plus(1) ?: 1,
                    text = "New Query"
                )

            copy(items = items + newItem)
        }
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

    private fun List<Query>.updateItem(id: Int, transformer: (Query) -> Query): List<Query> =
        map { item -> if (item.id == id) transformer(item) else item }

    private fun initialState(): RootState =
        RootState(
            items = (1..5).map { id ->
                Query(id = id, text = "Some text $id")
            },
        )

    private inline fun setState(update: RootState.() -> RootState) {
        state = state.update()
    }

    data class RootState(
        val items: List<Query> = emptyList(),
        val settings: Settings = Settings(),
        val activeRailItem: String = "",
        val editingItemId: Int? = null,
        val editingSettings: Boolean = false,
    )
}

