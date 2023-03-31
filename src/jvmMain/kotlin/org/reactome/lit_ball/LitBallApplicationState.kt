package org.reactome.lit_ball

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Notification
//import androidx.compose.ui.window.TrayState
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.window.LitBallWindowState

@Composable
fun rememberApplicationState() = remember {
    LitBallApplicationState().apply {
        newWindow()
    }
}

class LitBallApplicationState {
    val settings = Settings()

    private val _windows = mutableStateListOf<LitBallWindowState>()
    val windows: List<LitBallWindowState> get() = _windows

    fun newWindow() {
        _windows.add(
            LitBallWindowState(
                application = this,
                path = null,
                exit = _windows::remove
            )
        )
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    fun sendNotification(notification: Notification) {
//        tray.sendNotification(notification)
    }
}