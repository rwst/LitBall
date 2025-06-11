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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.Paper
import common.PaperList
import common.Tag
import dialog.RadioButtonOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import model.Filtering2RootStore
import util.SystemFunction
import util.openInBrowser
import util.setupLazyListScroller
import window.components.*

private const val TAG = "Filtering2MainContent"

@Suppress("FunctionName")
@Composable
internal fun Filtering2MainContent(
    scope: CoroutineScope,
    model: Filtering2RootStore,
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

                if (model.state.isClassifierSet)
                    Button(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        onClick = { model.state.paperListStore.setClassifierAlert(true) },
                    ) {
                        Text("Apply Classifier")
                    }
                Button(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    onClick = model::acceptAll,
                ) {
                    Text("Accept all")
                }
                Button(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    onClick = model::rejectAll,
                ) {
                    Text("Reject all")
                }
            }
            Filtering2ListContent(
                scope,
                items = PaperList.listHandle.getFullList(),
                onItemClicked = { model.state.paperListStore.onItemClicked(it) },
                onItemRadioButtonClicked = model::onItemRadioButtonClicked,
                lazyListState = lazyListState,
                focusRequester = focusRequester,
                setupListScroller = { model.state.paperListStore.setupListScroller(it) },
            )
        }
    }
}

@Composable
fun Filtering2ListContent(
    scope: CoroutineScope,
    items: List<Paper>,
    onItemClicked: (id: Long) -> Unit,
    onItemRadioButtonClicked: (id: Long, btn: Int) -> Unit,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    setupListScroller: (Channel<Long>) -> Unit,
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
                    .onPreviewKeyEvent(handleKeyPressed(lazyListState)),
                lazyListState
            ) {
                items(
                    key = { it.uniqueId },
                    items = items,
                ) { item ->
                    CardWithTextIconAndRadiobutton(
                        scope,
                        item = item,
                        onClicked = { onItemClicked(item.uniqueId) },
                        onOptionSelected = { btn ->
                            onItemRadioButtonClicked(item.uniqueId, btn)
                            focusRequester.requestFocus()
                        },
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
    scope: CoroutineScope,
    item: Paper,
    onClicked: () -> Unit,
    onOptionSelected: (btn: Int) -> Unit,
) {
    val cardTitle = item.details.title
    val year = item.details.publicationDate?.substringBefore("-") ?: ""
    val cardYear = if (year == "null") "" else year
    val isReview = item.details.publicationTypes?.contains("Review") ?: false
    val radioButtonOptions = Tag.entries.map { it.name }
    Card(
        elevation = 4.dp,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        backgroundColor = if (!isReview) Color.White else Color.LightGray,
    ) {
        Row(
            modifier = Modifier.padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = cardYear, modifier = Modifier.padding(start = 8.dp), fontSize = 12.sp)
                Tooltip(
                    text = "Open PubMed / Semantic Scholar\nin Browser",
                    Modifier.align(Alignment.CenterHorizontally)
                ) {
                    IconButton(
                        onClick = { scope.launch { openInBrowser(item) } },
                        modifier = Modifier.padding(0.dp)
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            .height(30.dp).width(48.dp)
                    ) {
                        Icon(
                            painterResource(Icons.Web),
                            contentDescription = "Open in Browser",
                            tint = Color.Blue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            paperTextComposable(
                cardTitle,
                modifier = Modifier
                    .weight(1F).align(Alignment.CenterVertically)
                    .clickable { onClicked() },
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

