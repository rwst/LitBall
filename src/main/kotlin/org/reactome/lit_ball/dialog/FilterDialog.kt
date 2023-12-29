@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.reactome.lit_ball.model.PaperListScreenStore

@Composable
private fun createWindow(
    store: PaperListScreenStore,
    content: @Composable (FrameWindowScope.() -> Unit)
) {
    Window(
        onCloseRequest = { store.setFilterDialog(false) },
        title = "Paper List Filter",
        state = rememberWindowState(
            position = WindowPosition(alignment = Alignment.TopEnd),
            size = DpSize(256.dp, 192.dp),
        ),
        content = content,
    )
}

@Composable
fun Filtering2FilterDialog(store: PaperListScreenStore) {
    createWindow(store) {}
}

@Composable
fun AnnotatingFilterDialog(store: PaperListScreenStore) {
    createWindow(store) {}
}