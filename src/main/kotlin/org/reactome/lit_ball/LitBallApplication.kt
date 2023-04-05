package org.reactome.lit_ball

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import org.reactome.lit_ball.window.LitBallWindow

@Suppress("FunctionName")
@Composable
fun LitBallApplication(state: LitBallApplicationState) {
    key(state.windows) {
        if (state.windows.isNotEmpty())
            LitBallWindow(state.windows[0])
    }
}
