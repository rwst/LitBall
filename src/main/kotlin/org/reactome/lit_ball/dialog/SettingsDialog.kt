@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import org.reactome.lit_ball.common.Settings

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SettingsDialog(
    onCloseClicked: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(Settings.map["path-to-queries"] ?: "") }

    AlertDialog(
        title = { Text("Edit settings") },
        onDismissRequest = onCloseClicked,
        confirmButton = {
            TextButton(
                onClick = {
                    Settings.map["path-to-queries"] = text
                    onCloseClicked.invoke()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCloseClicked
            ) {
                Text("Dismiss")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("path-to-queries") },
                    placeholder = { Text(Settings.map["path-to-queries"] ?: "") }
                )
            }
        },
    )
}
