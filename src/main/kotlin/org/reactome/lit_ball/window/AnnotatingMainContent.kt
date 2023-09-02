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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.dialog.FlagBoxes
import org.reactome.lit_ball.model.AnnotatingRootStore
import org.reactome.lit_ball.util.openInBrowser
import org.reactome.lit_ball.window.components.Rail
import org.reactome.lit_ball.window.components.RailItem
import org.reactome.lit_ball.window.components.Tooltip
import java.net.URI

@Suppress("FunctionName")
@Composable
internal fun AnnotatingMainContent(
    modifier: Modifier = Modifier,
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    railItems: List<RailItem>,
    onExit: () -> Unit,
    rootSwitch: MutableState<RootType>,
    isClassifierSet: Boolean,
    onClassifierButtonClicked: () -> Unit,
    onFlagSet: (Int, Int, Boolean) -> Unit
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
            isClassifierSet = isClassifierSet,
            onClassifierButtonClicked = onClassifierButtonClicked,
            onFlagSet = onFlagSet
        )
    }
}

@Composable
fun AnnotatingListContent(
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    isClassifierSet: Boolean,
    onClassifierButtonClicked: () -> Unit,
    onFlagSet: (Int, Int, Boolean) -> Unit
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
                    key = { it.hashCode() },
                    items = items,
                ) { item ->
                    CardWithFlagBoxes(
                        item = item,
                        onClicked = { onItemClicked(item.id) },
                        onFlagSet = { idx, value -> onFlagSet(item.id, idx, value) },
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
fun CardWithFlagBoxes(
    item: Paper,
    onClicked: () -> Unit,
    onFlagSet: (Int, Boolean) -> Unit,
) {
    val cardTitle = item.details.title
    val isReview = item.details.publicationTypes?.contains("Review") ?: false
    Card(
        elevation = 4.dp,
        backgroundColor = if (!isReview) Color.White else Color.LightGray,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tooltip(text = "Open Google Scholar\nin Browser")
            {
                IconButton(
                    onClick = {
                        AnnotatingRootStore.scope.launch(Dispatchers.IO) {
                            if (cardTitle != null) {
                                openInBrowser(URI("https://scholar.google.de/scholar?hl=en&as_sdt=0%2C5&q=${cardTitle.replace(" ","+")}&btnG="))
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Web,
                        contentDescription = "Open in Browser",
                        tint = Color.Blue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
            val fList = PaperList.flagList
            FlagBoxes(
                fList,
                item.flags,
                onFlagSet,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
