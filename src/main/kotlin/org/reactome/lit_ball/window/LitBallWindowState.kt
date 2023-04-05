package org.reactome.lit_ball.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowState
import org.reactome.lit_ball.LitBallApplicationState
import org.reactome.lit_ball.common.App
import org.reactome.lit_ball.common.Settings
import java.nio.file.Path

@Suppress("unused")
class LitBallWindowState(
    private val application: LitBallApplicationState,
    path: Path?,
    private val exit: (LitBallWindowState) -> Unit
) {
    val settings: Settings get() = application.settings

    val window = WindowState()

    var path by mutableStateOf(path)
        private set

    private var isChanged by mutableStateOf(false)

    private var _text by mutableStateOf("")

    var text: String
        get() = _text
        set(value) {
            check(isInit)
            _text = value
            isChanged = true
        }

    private var isInit by mutableStateOf(false)

    fun run() {
            initNew()
    }

    private fun initNew() {
        _text = ""
        isInit = true
        isChanged = false
        App.beforeWindow()
    }

    fun exit(): Boolean {
        App.afterWindow()
        exit(this)
        return true
    }
}