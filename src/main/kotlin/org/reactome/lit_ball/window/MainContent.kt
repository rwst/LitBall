@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.LitBallQuery
import org.reactome.lit_ball.common.QueryStatus

val MARGIN_SCROLLBAR: Dp = 0.dp

@Suppress("FunctionName")
@Composable
internal fun MainContent(
    modifier: Modifier = Modifier,
    qItems: List<LitBallQuery>,
    onItemClicked: (id: Int) -> Unit,
    onNewItemClicked: () -> Unit,
    railItems: List<RailItem>,
    onItemSettingsClicked: (id: Int?) -> Unit,
    onItemGoClicked: (status: QueryStatus, id: Int) -> Unit,
    rootSwitch: MutableState<Boolean>,
    onItemAnnotateClicked: (id: Int) -> Unit
) {
    Row(modifier) {
        Rail(
            railItems = railItems,
            onNewButtonClicked = onNewItemClicked,
            rootSwitch = rootSwitch,
        )
        Box(Modifier.weight(1F)) {
            ListContent(
                items = qItems,
                onItemClicked = onItemClicked,
                onItemSettingsClicked,
                onItemGoClicked,
                onItemAnnotateClicked,
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
    onItemAnnotateClicked: (id: Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .clickable { focusRequester.requestFocus() }
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(end = 12.dp),
            lazyListState
        ) {
            items(
                key = { it.hashCode() },
                items = items,
            ) { item ->
                QueryCard(
                    item = item,
                    onClicked = { onItemClicked(item.id) },
                    onItemSettingsClicked,
                    onGoClicked = onItemGoClicked,
                    onAnnotateClicked = { onItemAnnotateClicked(item.id) },
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