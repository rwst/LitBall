@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import RootType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.Paper
import org.reactome.lit_ball.common.PaperList
import org.reactome.lit_ball.common.Tag
import org.reactome.lit_ball.dialog.RadioButtonOptions
import org.reactome.lit_ball.model.Filtering2RootStore
import org.reactome.lit_ball.util.SystemFunction
import org.reactome.lit_ball.util.openInBrowser
import org.reactome.lit_ball.util.setupLazyListScroller
import org.reactome.lit_ball.window.components.*

private const val TAG = "Filtering2MainContent"

@Suppress("FunctionName")
@Composable
internal fun Filtering2MainContent(
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
                SortingControls(model.sortingControls, focusRequester)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {},
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(PaperList.fileName + " " + lazyListState.firstVisibleItemIndex.toString() + '/' + model.state.items.size.toString())
                }
                if (model.state.isClassifierSet)
                    Button(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        onClick = { model.state.paperListStore.setClassifierAlert(true) },
                    ) {
                        Text("Apply Classifier")
                    }
            }
            Filtering2ListContent(
                items = model.state.items,
                onItemClicked = { model.state.paperListStore.onItemClicked(it) },
                onItemRadioButtonClicked = model::onItemRadioButtonClicked,
                lazyListState = lazyListState,
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
fun Filtering2ListContent(
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    onItemRadioButtonClicked: (id: Int, btn: Int) -> Unit,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
) {
    val onKeyDown: (KeyEvent) -> Boolean = handleKeyPressed(lazyListState)

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent(onKeyDown)
    ) {
        setupLazyListScroller(TAG, rememberCoroutineScope(), lazyListState, Filtering2RootStore::setupListScroller)
        Column {
            LazyColumn(
                Modifier.fillMaxSize().padding(end = 12.dp),
                lazyListState
            ) {
                items(
                    key = { it.hashCode() },
                    items = items,
                ) { item ->
                    CardWithTextIconAndRadiobutton(
                        item = item,
                        onClicked = { onItemClicked(item.id) },
                        onOptionSelected = {
                            btn -> onItemRadioButtonClicked(item.id, btn)
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
    item: Paper,
    onClicked: () -> Unit,
    onOptionSelected: (btn: Int) -> Unit,
) {
    val cardTitle = item.details.title
    val cardPMID: String? = item.details.externalIds?.get("PubMed")
    val year = item.details.publicationDate?.substringBefore("-")?: ""
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
                Tooltip(text = "Open PubMed / Google Scholar\nin Browser", Modifier.align(Alignment.CenterHorizontally)) {
                    IconButton(
                        onClick = {
                            Filtering2RootStore.scope?.launch(Dispatchers.IO) {
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
            RadioButtonOptions(
                radioButtonOptions,
                item.tag.ordinal,
                onOptionSelected,
            )
        }
    }
}

