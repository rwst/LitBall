@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.Query
import org.reactome.lit_ball.common.QueryList

val MARGIN_SCROLLBAR: Dp = 0.dp
@Suppress("FunctionName")
@Composable
internal fun MainContent(
    modifier: Modifier = Modifier,
    items: QueryList,
    onExit: () -> Unit,
    onItemClicked: (id: Int) -> Unit,
    onItemDeleteClicked: (id: Int) -> Unit,
    onNewItemClicked: () -> Unit,
    onRailItemClicked: List<() -> Unit>,
) {
    Row(modifier) {
        Rail(
            onRailItemClicked = onRailItemClicked,
            onNewButtonClicked = onNewItemClicked,
            onExit = onExit,
        )

        Box(Modifier.weight(1F)) {
            ListContent(
                items = items,
                onItemClicked = onItemClicked,
                onItemDeleteClicked = onItemDeleteClicked
            )
        }
    }
}

@Composable
private fun ListContent(
    items: QueryList,
    onItemClicked: (id: Int) -> Unit,
    onItemDeleteClicked: (id: Int) -> Unit,
) {
    Box {
        val listState = rememberLazyListState()

        LazyColumn(state = listState) {
            items(items.list) { item ->
                Item(
                    item = item,
                    onClicked = { onItemClicked(item.id) },
                    onDeleteClicked = { onItemDeleteClicked(item.id) }
                )

                Divider()
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = listState)
        )
    }
}

@Composable
private fun Item(
    item: Query,
    onClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Row(modifier = Modifier.clickable(onClick = onClicked)) {
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = AnnotatedString(item.text),
            modifier = Modifier.weight(1F).align(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onDeleteClicked) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
    }
}
