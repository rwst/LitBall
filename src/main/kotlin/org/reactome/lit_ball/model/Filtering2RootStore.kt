package org.reactome.lit_ball.model

import RootType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.reactome.lit_ball.common.FileType
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.SortingControlItem
import org.reactome.lit_ball.window.components.SortingType

object Filtering2RootStore : ModelHandle {
    var state: Filtering2RootState by mutableStateOf(initialState())
    private var scrollChannel: Channel<Int>? = null

    override var scope: CoroutineScope? = null
    lateinit var rootSwitch: MutableState<RootType>

    fun switchRoot() {
        rootSwitch.value = RootType.MAIN_ROOT
    }

    private fun initialState(): Filtering2RootState = Filtering2RootState()

    private inline fun setState(update: Filtering2RootState.() -> Filtering2RootState) {
        state = state.update()
    }

    val railItems: List<RailItem> = listOf(
        RailItem("Save", "Save to ${FileType.ARCHIVED.fileName}", Icons.Save, 0) { doSave() },
        RailItem("Finish", "Finish filtering,\nwriting accepted/rejected", Icons.Done, 1) { doFinish() },
        RailItem(
            "Main",
            "Save and go back\nto main screen",
            Icons.ArrowBack,
            2,
            onClicked = { onDoAnnotateStopped() }),
        RailItem(
            "Exit",
            "Exit application",
            Icons.Logout,
            3,
            extraAction = SystemFunction.exitApplication,
            onClicked = { buttonExit() })
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
            "Publication date sort ascending",
            Icons.Sort12
        ) { doSort(SortingType.DATE_ASCENDING) },
        SortingControlItem(
            "Publication date sort descending",
            Icons.Sort21
        ) { doSort(SortingType.DATE_DESCENDING) },
    )

    override fun refreshList() {
        setState { copy(items = PaperList.toList()) }
    }

    override fun refreshClassifierButton() {
        setState { copy(isClassifierSet = PaperList.query.setting?.classifier?.isNotBlank() ?: false) }
    }

    override fun refreshStateFromPaperListScreenStore(paperListScreenStore: PaperListScreenStore) {
        setState { copy(paperListStore = paperListScreenStore) }
    }

    private fun buttonExit() {
        runBlocking {
            PaperList.save()
        }
    }

    fun onItemRadioButtonClicked(id: Int, btn: Int) {
        PaperList.setTag(id, btn)
        refreshList()
    }

    fun acceptAll() {
        PaperList.setAllTags(1)
        refreshList()
    }

    fun onDoAnnotateStopped() {
        runBlocking {
            PaperList.save()
        }
        state.paperListStore.setFilterDialog(false)
        switchRoot()
    }


    private fun doFinish() {
        scope?.launch(Dispatchers.IO) {
            PaperList.finish()
        }
    }

    private fun doSave() {
        scope?.launch(Dispatchers.IO) {
            PaperList.save()
        }
    }

    private fun doSort(sortingType: SortingType) {
        scope?.launch(Dispatchers.IO) {
            PaperList.sort(sortingType)
            refreshList()
            delay(100) // TODO: this is a hack
            scrollChannel?.send(0)
        }
    }

    fun setupListScroller(theChannel: Channel<Int>) {
        scrollChannel = theChannel
    }
}

data class Filtering2RootState(
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