import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.apache.log4j.BasicConfigurator
import org.reactome.lit_ball.window.AnnotatingRootContent
import org.reactome.lit_ball.window.RootContent

fun main() {
    BasicConfigurator.configure()
    application {
        val rootSwitch = remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "LitBall",
            state = rememberWindowState(
                position = WindowPosition(alignment = Alignment.Center),
//                size = DpSize(1280.dp, 768.dp),
            ),
        ) {
            MaterialTheme {
                if (rootSwitch.value) {
                    AnnotatingRootContent(
                        modifier = Modifier.fillMaxSize(),
                        onExit = ::exitApplication,
                        rootSwitch,
                    )
                }
                else {
                    RootContent(
                        modifier = Modifier.fillMaxSize(),
                        onExit = ::exitApplication,
                        rootSwitch,
                    )
                }
            }
        }
    }
}
