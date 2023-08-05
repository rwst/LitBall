import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.reactome.lit_ball.window.RootContent

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "LitBall",
            state = rememberWindowState(
                position = WindowPosition(alignment = Alignment.Center),
//                size = DpSize(1280.dp, 768.dp),
            ),
        ) {
            MaterialTheme {
                RootContent(
                    modifier = Modifier.fillMaxSize(),
                    onExit = ::exitApplication,
                )
            }
        }
    }
}
