@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.reactome.lit_ball.model.PaperListScreenStore
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.Tooltip

@Composable
private fun createWindow(
    store: PaperListScreenStore,
    textField: MutableState<String>,
    content: @Composable (FrameWindowScope.() -> Unit)
) {
    Window(
        onCloseRequest = { store.setFilterDialog(false) },
        title = "Paper List Filter",
        state = rememberWindowState(
            position = WindowPosition(alignment = Alignment.TopEnd),
            size = DpSize(320.dp, 192.dp),
        ),
        content = {
            AlertDialog(
                title = { Text("Filter for substring in title/abstract") },
                onDismissRequest = {},
                confirmButton = {},
                dismissButton = {},
                text = {
                    Column {
                        Row {
                            Tooltip("Set Filter", Modifier.align(Alignment.CenterVertically)) {
                                TextField(
                                    value = textField.value,
                                    modifier = Modifier.fillMaxWidth(fraction = 0.85f),
                                    onValueChange = {
                                        textField.value = it
                                        store.onFilterChanged(textField.value)
                                    },
                                    label = null,
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Tooltip(
                                if (textField.value.isEmpty()) "Activate filter" else "Remove filter",
                                Modifier.align(Alignment.CenterVertically)
                            ) {
                                IconButton(
                                    onClick = {
                                        textField.value = ""
                                        store.onFilterChanged(textField.value)
                                        //focusRequester.requestFocus()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically),
                                ) {
                                    Icon(
                                        painterResource(if (textField.value.isEmpty()) Icons.Search else Icons.Cancel),
                                        contentDescription = null,
                                        tint = Color.Blue,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row {
                            content()
                        }
                    }
                },
            )
        }
    )
}

@Composable
fun AnnotatingFilterDialog(store: PaperListScreenStore) {
    val textField = rememberSaveable { mutableStateOf("") }
    createWindow(store, textField) {
        Button(
            onClick = store::onRemoveFiltered,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                "Remove all",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun Filtering2FilterDialog(store: PaperListScreenStore) {
    val textField = rememberSaveable { mutableStateOf("") }
    createWindow(store, textField) {
        Button(
            onClick = { store.onAcceptFiltered(true) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                "Accept all",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Button(
            onClick = { store.onAcceptFiltered(false) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                "Reject all",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}