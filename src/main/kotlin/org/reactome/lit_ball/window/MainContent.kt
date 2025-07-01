@file:Suppress("FunctionName")

package window

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import common.LitBallQuery
import common.QueryList
import common.QueryStatus
import model.RootStore
import util.setupLazyListScroller
import window.components.*

val MARGIN_SCROLLBAR: Dp = 0.dp
private const val TAG = "MainContent"

@Composable
internal fun MainContent(
    model: RootStore,
    rootSwitch: MutableState<RootType>,
    focusRequester: FocusRequester,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Rail(
            railItems = model.railItems,
            onNewButtonClicked = { model.setNewItem(true) },
            rootSwitch = rootSwitch,
        )
        Column {
            Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                SortingControls(model.sortingControls)
                Spacer(modifier = Modifier.width(8.dp))
                Tooltip("Refresh from disk", Modifier.align(Alignment.CenterVertically)) {
                    TextButton(
                        onClick = { model.onQueryPathClicked(true) },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        model.state.queryPath?.let { Text(it) }
                    }
                }
            }
            ListContent(
                model = model,
                items = remember { QueryList.list },
                onItemClicked = { id -> model.setEditingItemId(id) },
                onItemSettingsClicked = { id -> model.onQuerySettingsClicked(id) },
                onItemGoClicked = { status, id -> model.nextAction(status, id) },
                onItemAnnotateClicked = { id -> model.onAnnotateStarted(id) },
                onDeleteClicked = { id -> model.onDeleteQueryClicked(id) },
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
private fun ListContent(
    model: RootStore,
    items: List<LitBallQuery>,
    onItemClicked: (id: Long) -> Unit,
    onItemSettingsClicked: (id: Long?) -> Unit,
    onItemGoClicked: (status: QueryStatus, id: Long) -> Unit,
    onItemAnnotateClicked: (id: Long) -> Unit,
    onDeleteClicked: (id: Long) -> Unit,
    focusRequester: FocusRequester,
) {
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .clickable { focusRequester.requestFocus() }
    ) {
        setupLazyListScroller(TAG, rememberCoroutineScope(), lazyListState, model::setupListScroller)
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(end = 12.dp)
                .onPreviewKeyEvent(handleKeyPressed(lazyListState)),
            lazyListState
        ) {
            items(
                key = { it.id },
                items = items,
            ) { item ->
                QueryCard(
                    item = item,
                    onClicked = { onItemClicked(item.id) },
                    onItemSettingsClicked,
                    onGoClicked = onItemGoClicked,
                    onAnnotateClicked = { onItemAnnotateClicked(item.id) },
                    onDeleteClicked = { onDeleteClicked(item.id) },
                )
                Divider()
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(
                scrollState = lazyListState
            )
        )
    }
}