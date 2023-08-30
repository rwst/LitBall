import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.apache.log4j.BasicConfigurator
import org.reactome.lit_ball.window.AnnotatingRootContent
import org.reactome.lit_ball.window.Filtering2RootContent
import org.reactome.lit_ball.window.RootContent
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream

enum class RootType { MAIN_ROOT, FILTER2_ROOT, ANNOTATE_ROOT }
fun main() {
    BasicConfigurator.configure()
    application {
        val rootSwitch = remember { mutableStateOf(RootType.MAIN_ROOT) }
        val appIcon = remember {
            System.getProperty("app.dir")
                ?.let { Paths.get(it, "icon-512.png") }
                ?.takeIf { it.exists() }
                ?.inputStream()
                ?.buffered()
                ?.use { BitmapPainter(loadImageBitmap(it)) }
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "LitBall",
            icon = appIcon,
            state = rememberWindowState(
                position = WindowPosition(alignment = Alignment.Center),
                size = DpSize(1024.dp, 768.dp),
            ),
        ) {
            MaterialTheme {
                when (rootSwitch.value) {
                    RootType.MAIN_ROOT -> RootContent(
                        modifier = Modifier.fillMaxSize(),
                        onExit = ::exitApplication,
                        rootSwitch,
                    )
                    RootType.FILTER2_ROOT -> Filtering2RootContent(
                        modifier = Modifier.fillMaxSize(),
                        onExit = ::exitApplication,
                        rootSwitch,
                    )
                    RootType.ANNOTATE_ROOT -> AnnotatingRootContent(
                        modifier = Modifier.fillMaxSize(),
                        onExit = ::exitApplication,
                        rootSwitch,
                    )
                }
            }
        }
    }
}
