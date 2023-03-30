package org.reactome.lit_ball

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.window.ApplicationScope
import org.reactome.lit_ball.window.LitBallWindow

@Composable
fun ApplicationScope.LitBallApplication(state: LitBallApplicationState) {
    key(state.windows) {
        if (state.windows.isNotEmpty())
            LitBallWindow(state.windows[0])
    }
}
