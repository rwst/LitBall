package org.reactome.lit_ball.window
import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.LocalAppResources

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

        LitBallRail()
    }
}
