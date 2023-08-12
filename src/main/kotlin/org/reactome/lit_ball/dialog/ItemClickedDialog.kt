package org.reactome.lit_ball.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.PaperList

@OptIn(ExperimentalMaterialApi::class)
@Suppress("FunctionName")
@Composable
// TODO: Fix Scrolling
internal fun ItemClickedDialog(id: Int, onDoneClicked: () -> Unit) {
    val lazyListState = rememberLazyListState()
    AlertDialog(
        modifier = Modifier
            .fillMaxSize(fraction = 0.8f)
            .padding(12.dp),
        title = {
            Text(text = "Paper# $id")
        },
        text = {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Row {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(end = 12.dp).size(100.dp),
                        lazyListState
                    ) {
                        item {
                            SelectionContainer {
                                Text(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    text = PaperList.pretty(id),
                                )
                            }
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
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = { onDoneClicked() },
            ) {
                Text("Back")
            }
        },
    )
}
