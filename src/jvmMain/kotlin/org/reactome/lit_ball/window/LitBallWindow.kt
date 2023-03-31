package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.LocalAppResources
import org.reactome.lit_ball.util.FileDialog
import org.reactome.lit_ball.util.YesNoCancelDialog

@Suppress("FunctionName")
@Composable
fun LitBallWindow(state: LitBallWindowState) {
    val scope = rememberCoroutineScope()

    fun exit() = scope.launch { state.exit() }

    Window(
        state = state.window,
        title = "LitBall",
        icon = LocalAppResources.current.icon,
        onCloseRequest = { exit() }
    ) {
        LaunchedEffect(Unit) { state.run() }

//        WindowNotifications(state)
//        WindowMenuBar(state)

        // TextField isn't efficient for big text files, we use it for simplicity
        BasicTextField(
            state.text,
            state::text::set,
            enabled = state.isInit,
            modifier = Modifier.fillMaxSize()
        )

        if (state.openDialog.isAwaiting) {
            FileDialog(
                title = "Notepad",
                isLoad = true,
                onResult = {
                    state.openDialog.onResult(it)
                }
            )
        }

        if (state.saveDialog.isAwaiting) {
            FileDialog(
                title = "Notepad",
                isLoad = false,
                onResult = { state.saveDialog.onResult(it) }
            )
        }

        if (state.exitDialog.isAwaiting) {
            YesNoCancelDialog(
                title = "Notepad",
                message = "Save changes?",
                onResult = { state.exitDialog.onResult(it) }
            )
        }
    }
}

