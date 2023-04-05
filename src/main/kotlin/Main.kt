import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import org.reactome.lit_ball.LitBallApplication
import org.reactome.lit_ball.common.LocalAppResources
import org.reactome.lit_ball.common.rememberAppResources
import org.reactome.lit_ball.rememberApplicationState

fun main() = application {
    CompositionLocalProvider(LocalAppResources provides rememberAppResources()) {
        LitBallApplication(rememberApplicationState())
    }
}
