@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.Settings

@Composable
internal fun SettingsDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit
) {
    val keys = Settings.map.keys.toList()
    val advancedKeys = Settings.advancedSet
    val textFields = rememberSaveable { keys.map { key -> mutableStateOf(Settings.map[key] ?: "") } }
    val isAdvancedSettingsVisible = remember { mutableStateOf(false) }

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
            Column(horizontalAlignment = Alignment.Start) {
                keys.forEachIndexed { index, key ->
                    if (key !in advancedKeys) {
                        TextField(
                            value = textFields[index].value,
                            onValueChange = { textFields[index].value = it },
                            label = { Text(key) },
                            placeholder = { Text(Settings.map[key] ?: "") }
                        )
                    }
                }
                Text(
                    text = "Advanced Settings",
                    modifier = Modifier
                        .clickable { isAdvancedSettingsVisible.value = !isAdvancedSettingsVisible.value }
                        .padding(16.dp)
                )

                if (isAdvancedSettingsVisible.value) {
                    keys.forEachIndexed { index, key ->
                        if (key in advancedKeys) {
                            TextField(
                                value = textFields[index].value,
                                onValueChange = { textFields[index].value = it },
                                label = { Text(key) },
                                placeholder = { Text(Settings.map[key] ?: "") }
                            )
                        }
                    }
                }
            }
        },
    )
}
