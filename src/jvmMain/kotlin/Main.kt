import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.mapdb.DB
import org.mapdb.DBMaker

@Composable
@Preview
fun app() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    val db = DBMaker.memoryDB().make()
    val map = DB.HashMapMaker<String, String>(db, "map").createOrOpen()
    map["a"] = "a"
    db.close()

    Window(onCloseRequest = ::exitApplication) {
        app()
    }
}
