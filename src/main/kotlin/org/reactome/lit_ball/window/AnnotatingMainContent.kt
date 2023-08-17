@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Tag
import org.reactome.lit_ball.dialog.RadioButtonOptions

@Suppress("FunctionName")
@Composable
internal fun AnnotatingMainContent(
    modifier: Modifier = Modifier,
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    railItems: List<RailItem>,
    onItemDeleteClicked: (id: Int) -> Unit,
    onItemRadioButtonClicked: (id: Int, btn: Int) -> Unit,
    onExit: () -> Unit,
    rootSwitch: MutableState<Boolean>,
    isClassifierSet: Boolean,
    onClassifierButtonClicked: () -> Unit,
    ) {
    Row(modifier) {
        Rail(
            railItems = railItems,
            onExit,
            rootSwitch = rootSwitch,
        )

        AnnotatingListContent(
            items = items,
            onItemClicked = onItemClicked,
            onItemDeleteClicked = onItemDeleteClicked,
            onItemRadioButtonClicked = onItemRadioButtonClicked,
            isClassifierSet = isClassifierSet,
            onClassifierButtonClicked = onClassifierButtonClicked,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AnnotatingListContent(
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    onItemDeleteClicked: (id: Int) -> Unit,
    onItemRadioButtonClicked: (id: Int, btn: Int) -> Unit,
    isClassifierSet: Boolean,
    onClassifierButtonClicked: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    val onKeyDownSuspend: suspend (KeyEvent) -> Boolean = {
        when (it.type) {
            KeyEventType.KeyUp -> false

            else -> {
                val topItem = lazyListState.firstVisibleItemIndex
                val topOffset = lazyListState.firstVisibleItemScrollOffset
                when (it.key) {
                    Key.DirectionUp -> {
                        if (topOffset > 0)
                            lazyListState.scrollToItem(topItem)
                        else if (topItem > 0)
                            lazyListState.scrollToItem(topItem - 1)
                        if (topOffset > 0)
                            lazyListState.scrollToItem(topItem)
                        else if (topItem > 0)
                            lazyListState.scrollToItem(topItem - 1)
                        true
                    }

                    Key.DirectionDown -> {
                        lazyListState.scrollToItem(topItem + 1)
                        true
                    }

                    else -> false
                }
            }
        }
    }
    val onKeyDown: (KeyEvent) -> Boolean = {
        runBlocking { onKeyDownSuspend(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .clickable { focusRequester.requestFocus() }
            .onPreviewKeyEvent(onKeyDown)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {},
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(PaperList.fileName + " " + lazyListState.firstVisibleItemIndex.toString() + '/' + items.size.toString())
                }
                Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
                if (isClassifierSet)
                    Button(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        onClick = onClassifierButtonClicked,
                    ) {
                        Text("Apply Classifier")
                    }
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(end = 12.dp),
                lazyListState
            ) {
                items(
                    key = { it.id },
                    items = items,
                ) { item ->
                    CardWithTextIconAndRadiobutton(
                        item = item,
                        onClicked = { onItemClicked(item.id) },
                        onDeleteClicked = { onItemDeleteClicked(item.id) },
                        onOptionSelected = { btn -> onItemRadioButtonClicked(item.id, btn) },
                    )
                    Divider()
                }
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

@Composable
fun CardWithTextIconAndRadiobutton(
    item: Paper,
    onClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onOptionSelected: (btn: Int) -> Unit,
) {
    val cardTitle = item.details.title
    val radioButtonOptions = Tag.entries.map { it.name }
    Card(
        elevation = 4.dp,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = cardTitle ?: "",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1F).align(Alignment.CenterVertically)
                    .clickable { onClicked() },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(16.dp))
            RadioButtonOptions(
                radioButtonOptions,
                item.tag.ordinal,
                onOptionSelected,
            )
        }
    }
}

