package org.reactome.lit_ball.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.*
import org.reactome.lit_ball.LitBallApplicationState
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.util.AlertDialogResult
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

    var isChanged by mutableStateOf(false)
        private set

    val openDialog = DialogState<Path?>()
    val saveDialog = DialogState<Path?>()
    val exitDialog = DialogState<AlertDialogResult>()

    private var _text by mutableStateOf("")

    var text: String
        get() = _text
        set(value) {
            check(isInit)
            _text = value
            isChanged = true
        }

    var isInit by mutableStateOf(false)
        private set

    fun run() {
//     suspend fun run() {
//        if (path != null) {
//            open(path!!)
//        } else {
            initNew()
//        }
    }

    private fun initNew() {
        _text = ""
        isInit = true
        isChanged = false
    }

    fun exit(): Boolean {
        exit(this)
        return true
    }

//    fun sendNotification(notification: Notification) {
//        application.sendNotification(notification)
//    }
}

//@OptIn(DelicateCoroutinesApi::class)
//private fun Path.launchSaving(text: String) = GlobalScope.launch {
//    writeTextAsync(text)
//}
//
//private suspend fun Path.writeTextAsync(text: String) = withContext(Dispatchers.IO) {
//    toFile().writeText(text)
//}

//sealed class LitBallWindowNotification {
////    class SaveSuccess(val path: Path) : LitBallWindowNotification()
////    class SaveError(val path: Path) : LitBallWindowNotification()
//}

class DialogState<T> {
    private var onResult: CompletableDeferred<T>? by mutableStateOf(null)

    val isAwaiting get() = onResult != null

    fun onResult(result: T) = onResult!!.complete(result)
}