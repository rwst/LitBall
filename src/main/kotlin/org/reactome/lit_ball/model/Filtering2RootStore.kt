package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings

object Filtering2RootStore: ModelHandle {
    var state: Filtering2RootState by mutableStateOf(initialState())

    var scope: CoroutineScope? = null
    set(value) {
        if (value != null) {
            state.paperListStore.scope = value
        }
        field = value
    }
    lateinit var rootSwitch: MutableState<RootType>

    fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun initialState(): Filtering2RootState = Filtering2RootState()

    private inline fun setState(update: Filtering2RootState.() -> Filtering2RootState) {
        state = state.update()
    }

    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query?.setting?.classifier?.isNotBlank() ?: false) }
    }

    override fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore) {
        setState { copy(paperListStore = paperListScreenStore) }
    }

    fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }

    fun onItemRadioButtonClicked(id: Int, btn: Int) {
        PaperList.setTag(id, btn)
        refreshList()
    }

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.save()
        }
        switchRoot()
    }


    fun doFinish(doit: Boolean) {
        if (doit) {
            scope?.launch(Dispatchers.IO) {
                PaperList.finish()
            }
        }
    }

    fun setDoSave(doSave: Boolean) {
        if (doSave) {
            scope?.launch(Dispatchers.IO) {
                PaperList.save()
            }
        }
    }
}

data class Filtering2RootState (
    val items: List<Paper> = PaperList.toList(),
    val settings: Settings = Settings,
    val activeRailItem: String = "",
    val editingSettings: Boolean = false,
    val infoList: Boolean = false,
    val newList: Boolean = false,
    val openList: Boolean = false,
    val doImport: Boolean = false,
    val isClassifierSet: Boolean = false,
    val paperListStore: PaperListScreenStore = PaperListScreenStore(Filtering2RootStore),
)