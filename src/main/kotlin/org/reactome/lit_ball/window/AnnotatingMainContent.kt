@file:Suppress("FunctionName")

package window

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import model.AnnotatingRootStore
import common.Paper
import common.PaperList
import dialog.FlagBoxes
import util.SystemFunction
import util.openInBrowser
import util.setupLazyListScroller
import window.components.*

private const val TAG = "AnnotatingMainContent"

@Suppress("FunctionName")
@Composable
internal fun AnnotatingMainContent(
    model: AnnotatingRootStore,
    rootSwitch: MutableState<RootType>,
    focusRequester: FocusRequester,
) {
    val lazyListState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxSize()) {
        Rail(
            railItems = model.railItems,
            SystemFunction.exitApplication,
            rootSwitch = rootSwitch,
        )

        Column {
            Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                paperListHeader(
                    model.state.paperListStore,
                    focusRequester,
                    lazyListState,
                    Modifier.align(Alignment.CenterVertically),
                )
            }
            AnnotatingListContent(
                items = model.state.paperListState.items,
                onItemClicked = { model.state.paperListStore.onItemClicked(it) },
                onFlagSet = model::onFlagSet,
                lazyListState = lazyListState,
                focusRequester = focusRequester,
                setupListScroller = { model.state.paperListStore.setupListScroller(it) },
            )
        }
    }
}

@Composable
fun AnnotatingListContent(
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    onFlagSet: (Int, Int, Boolean) -> Unit,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    setupListScroller: (Channel<Int>) -> Unit,
) {
    val onKeyDown: (KeyEvent) -> Boolean = handleKeyPressed(lazyListState)

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent(onKeyDown)
    ) {
        setupLazyListScroller(TAG, rememberCoroutineScope(), lazyListState, setupListScroller)
        Column {
            LazyColumn(
                Modifier.fillMaxSize()
                    .padding(end = 12.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent(handleKeyPressed(lazyListState)),
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
    val model = AnnotatingRootStore
    val cardTitle = item.details.title
    val cardPMID: String? = item.details.externalIds?.get("PubMed")
    val year = item.details.publicationDate?.substringBefore("-") ?: ""
    val cardYear = if (year == "null") "" else year
    val isReview = item.details.publicationTypes?.contains("Review") ?: false
    val hasFlagSet = item.flags.isNotEmpty()
    Card(
        elevation = 4.dp,
        backgroundColor = when {
            !isReview && !hasFlagSet -> Color.White
            !isReview && hasFlagSet -> Color(red = 233, green = 233, blue = 233)
            isReview && !hasFlagSet -> Color.LightGray
            else -> Color(red = 189, green = 189, blue = 189)
        },
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = cardYear, modifier = Modifier.padding(start = 8.dp), fontSize = 12.sp)
                Tooltip(text = "Open PubMed / Google Scholar\nin Browser", Modifier.align(Alignment.CenterHorizontally))
                {
                    IconButton(
                        onClick = {
                            AnnotatingRootStore.scope?.launch(Dispatchers.IO) {
                                openInBrowser(cardPMID, cardTitle)
                            }
                        },
                        modifier = Modifier.padding(0.dp)
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .height(30.dp).width(48.dp)
                    ) {
                        Icon(
                            painterResource(Icons.Web),
                            contentDescription = "Open in Browser",
                            tint = Color.Blue,
                            modifier = Modifier.size(18.dp).padding(0.dp)
                        )
                    }
                }
            }
            paperTextComposable(
                cardTitle,
                Modifier
                    .weight(1F)
                    .align(Alignment.CenterVertically)
                    .clickable { onClicked() },
            )
            Spacer(modifier = Modifier.width(16.dp))
            val fList = PaperList.flagList
            FlagBoxes(
                fList,
                item.flags,
                onFlagSet,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Tooltip("Remove paper from accepted", Modifier.align(Alignment.CenterVertically)) {
                IconButton(
                    onClick = { model.deleteClicked(item.id) },
                    modifier = Modifier
                        .size(height = 30.dp, width = 30.dp)
                        .align(Alignment.CenterVertically)
                        .focusProperties { canFocus = false },
                ) {
                    Icon(
                        painter = painterResource(Icons.Delete),
                        contentDescription = "Remove Query",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp).align(Alignment.CenterVertically),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
