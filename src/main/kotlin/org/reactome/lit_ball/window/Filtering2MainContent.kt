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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import org.reactome.lit_ball.window.components.Rail
import org.reactome.lit_ball.window.components.SortingControls
import org.reactome.lit_ball.window.components.Tooltip
import org.reactome.lit_ball.window.components.handleKeyPressed
import java.net.URI

private const val TAG = "Filtering2MainContent"

@Suppress("FunctionName")
@Composable
internal fun Filtering2MainContent(
    model: Filtering2RootStore,
    rootSwitch: MutableState<RootType>,
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
                SortingControls(model.sortingControls)
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {},
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(PaperList.fileName + " " + lazyListState.firstVisibleItemIndex.toString() + '/' + model.state.items.size.toString())
                }
            }
            Filtering2ListContent(
                items = model.state.items,
                onItemClicked = { model.state.paperListStore.onItemClicked(it) },
                onItemRadioButtonClicked = model::onItemRadioButtonClicked,
                isClassifierSet = model.state.isClassifierSet,
                onClassifierButtonClicked = { model.state.paperListStore.setClassifierAlert(true) },
                lazyListState = lazyListState,
            )
        }
    }
}

@Composable
fun Filtering2ListContent(
    items: List<Paper>,
    onItemClicked: (id: Int) -> Unit,
    onItemRadioButtonClicked: (id: Int, btn: Int) -> Unit,
    isClassifierSet: Boolean,
    onClassifierButtonClicked: () -> Unit,
    lazyListState: LazyListState
) {
    val focusRequester = remember { FocusRequester() }

    val onKeyDown: (KeyEvent) -> Boolean = handleKeyPressed(lazyListState)

    Box(
        modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .clickable { focusRequester.requestFocus() }
            .onPreviewKeyEvent(onKeyDown)
    ) {
        setupLazyListScroller(TAG, rememberCoroutineScope(), lazyListState, Filtering2RootStore::setupListScroller)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                    CardWithTextIconAndRadiobutton(
                        item = item,
                        onClicked = { onItemClicked(item.id) },
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
    onOptionSelected: (btn: Int) -> Unit,
) {
    val cardTitle = item.details.title
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
            Tooltip(text = "Open Google Scholar\nin Browser") {
                IconButton(
                    onClick = {
                        Filtering2RootStore.scope?.launch(Dispatchers.IO) {
                            if (cardTitle != null) {
                                openInBrowser(
                                    URI(
                                        "https://scholar.google.de/scholar?hl=en&as_sdt=0%2C5&q=${
                                            cardTitle.replace(
                                                " ",
                                                "+"
                                            )
                                        }&btnG="
                                    )
                                )
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

