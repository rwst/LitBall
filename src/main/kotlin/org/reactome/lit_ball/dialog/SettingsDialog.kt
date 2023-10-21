@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.window.components.Tooltip
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.isWritable

@Composable
internal fun SettingsDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit
) {
    val keys = Settings.map.keys.toList()
    val advancedKeys = Settings.advancedSet
    val textFields = rememberSaveable { keys.map { key -> mutableStateOf(Settings.map[key] ?: "") } }
    val isAdvancedSettingsVisible = remember { mutableStateOf(false) }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }

    AlertDialog(
        title = { Text("Edit settings") },
        onDismissRequest = onCloseClicked,
        confirmButton = {
            TextButton(
                onClick = {
                    pathWarningValue.value = null
                    val dirFile = File(textFields[0].value)
                    if (!dirFile.exists() && !Path(dirFile.parent).isWritable()) {
                        pathWarningValue.value = "Query directory cannot be created. Please change value or permissions"
                        return@TextButton
                    }
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
                        Row {
                            TextField(
                                value = textFields[index].value,
                                onValueChange = { textFields[index].value = it },
                                label = { Text(key) },
                                placeholder = { Text(Settings.map[key] ?: "") }
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Tooltip(text = Settings.helpText[key]?: key) {
                                Icon(
                                    painterResource(org.reactome.lit_ball.window.components.Icons.Help),
                                    contentDescription = "Query Settings",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                        .align(Alignment.CenterVertically),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            if (index == 0) {
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
                        }
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
