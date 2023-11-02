@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.LitBallQuery
import org.reactome.lit_ball.common.QueryStatus
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.setupLazyListScroller
import org.reactome.lit_ball.window.components.*

val MARGIN_SCROLLBAR: Dp = 0.dp
private const val TAG = "MainContent"

@Composable
internal fun MainContent(
    model: RootStore,
    rootSwitch: MutableState<RootType>,
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
                        onClick = model::onQueryPathClicked,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        model.state.queryPath?.let { Text(it) }
                    }
                }
            }
            ListContent(
                items = model.state.items,
                onItemClicked = { id -> model.setEditingItemId(id) },
                onItemSettingsClicked = { id -> model.onQuerySettingsClicked(id) },
                onItemGoClicked = { status, id -> model.nextAction(status, id) },
                onItemAnnotateClicked = { id -> model.onAnnotateStarted(id) },
                onDeleteClicked = { id -> model.onDeleteQueryClicked(id) }
            )
        }
    }
}

@Composable
private fun ListContent(
    items: List<LitBallQuery>,
    onItemClicked: (id: Int) -> Unit,
    onItemSettingsClicked: (id: Int?) -> Unit,
    onItemGoClicked: (status: QueryStatus, id: Int) -> Unit,
    onItemAnnotateClicked: (id: Int) -> Unit,
    onDeleteClicked: (id: Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .clickable { focusRequester.requestFocus() }
    ) {
        setupLazyListScroller(TAG, rememberCoroutineScope(), lazyListState, RootStore::setupListScroller)
        LazyColumn(
            Modifier.fillMaxSize().padding(end = 12.dp),
            lazyListState
        ) {
            items(
                key = {
                    try { it.hashCode() }
                    catch (e: ConcurrentModificationException) {
                        Logger.error(e)
                    } },
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