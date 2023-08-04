@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.Settings

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SettingsDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit
) {
    val keys = Settings.map.keys.toList()
    val textFields = rememberSaveable { keys.map { key -> mutableStateOf(Settings.map[key] ?: "") } }

    AlertDialog(
        title = { Text("Edit settings") },
        onDismissRequest = onCloseClicked,
        confirmButton = {
            TextButton(
                onClick = {
                    keys.forEachIndexed { index, key ->
                        Settings.map[key] = textFields[index].value
                    }
                    rootScope.launch(Dispatchers.IO) {
                        Settings.save()
                    }
                    onCloseClicked()
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
                keys.forEachIndexed { index, key ->
                    TextField(
                        value = textFields[index].value,
                        onValueChange = { textFields[index].value = it },
                        label = { Text(key) },
                        placeholder = { Text(Settings.map[key] ?: "") }
                    )
                }
            }
        },
    )
}