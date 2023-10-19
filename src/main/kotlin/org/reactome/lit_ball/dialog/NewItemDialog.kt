@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.QueryList
import org.reactome.lit_ball.common.Settings
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.isWritable


@Composable
fun NewItemDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val fieldValue = rememberSaveable { mutableStateOf("") }
    val nameValue = rememberSaveable { mutableStateOf("") }
    val checkValue = rememberSaveable { mutableStateOf(true) }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }

    AlertDialog(
        onDismissRequest = { (onCloseClicked)() },
        confirmButton = {
            TextButton(
                onClick = {
                    val dois = fieldValue.value.split("\n")
                        .map { it.trim() }
                        .toSet()
                    val name = nameValue.value.trim()
                    pathWarningValue.value = null
                    val queryPath = Settings.map["path-to-queries"] ?: ""
                    if (File(queryPath).exists() && !Path(queryPath).isWritable()) {
                        pathWarningValue.value = "Query directory is not writable"
                        return@TextButton
                    }
                    checkValue.value = dois.isNotEmpty() && name.isNotEmpty()
                    if (checkValue.value) {
                        rootScope.launch(Dispatchers.IO) {
                            QueryList.addNewItem(name, dois)
                        }
                        (onCloseClicked)()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { (onCloseClicked)() },
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text(
                "Create new query",
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Row {
                    TextField(
                        value = nameValue.value,
                        onValueChange = { nameValue.value = it },
                        label = { Text("Query name") },
                    )
                    pathWarningValue.value?.also {
                        Text(
                            it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                        )
                    }
                }
                TextField(
                    value = fieldValue.value,
                    onValueChange = {
                        fieldValue.value = it.transformDOI()
                        pathWarningValue.value = null
                                    },
                    label = { Text("Core DOIs (one per line)") },
                    placeholder = { Text("10.XYZ/ABC\n10.XYZ/ABC") }
                )
                if (!checkValue.value)
                    Text("Please fill both text fields.")
            }
        },
    )
}

private fun String.transformDOI(): String {
    var s = this.uppercase()
    if (s.startsWith("HTTP")) {
       s = URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
    }
    return s.removePrefix("HTTPS://DOI.ORG/")
}
