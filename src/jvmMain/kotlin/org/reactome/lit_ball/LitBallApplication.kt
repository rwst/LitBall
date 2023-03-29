package org.reactome.lit_ball

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.window.ApplicationScope
import org.reactome.lit_ball.window.LitBallWindow

@Composable
fun ApplicationScope.LitBallApplication(state: LitBallApplicationState) {
//    if (state.settings.isTrayEnabled && state.windows.isNotEmpty()) {
//        ApplicationTray(state)
//    }

    for (window in state.windows) {
        key(window) {
            LitBallWindow(window)
        }
    }
}

//@Composable
//private fun ApplicationScope.ApplicationTray(state: LitBallApplicationState) {
//    Tray(
//        LocalAppResources.current.icon,
//        state = state.tray,
//        tooltip = "Notepad",
//        menu = { ApplicationMenu(state) }
//    )
//}

//@Composable
//private fun MenuScope.ApplicationMenu(state: LitBallApplicationState) {
//    val scope = rememberCoroutineScope()
//    fun exit() = scope.launch { state.exit() }
//
//    Item("New", onClick = state::newWindow)
//    Separator()
//    Item("Exit", onClick = { exit() })
//}