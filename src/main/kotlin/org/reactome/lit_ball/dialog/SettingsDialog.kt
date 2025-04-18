@file:Suppress("FunctionName")

package dialog

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
import common.Settings
import window.components.Tooltip
import window.components.Icons
import java.io.File

@Composable
internal fun SettingsDialog(
    rootScope: CoroutineScope,
    onCloseClicked: suspend () -> Unit
) {
    val keys = Settings.map.keys.toList()
    val advancedKeys = Settings.advancedSet
    val textFields = rememberSaveable { keys.map { key -> mutableStateOf(Settings.map[key] ?: "") } }
    val isAdvancedSettingsVisible = remember { mutableStateOf(false) }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }

    AlertDialog(
        title = { Text("Edit settings") },
        onDismissRequest = { CoroutineScope(Dispatchers.IO).launch { onCloseClicked() } },
        confirmButton = {
            TextButton(
                onClick = {
                    pathWarningValue.value = null
                    val dirFile = Settings.map["path-to-queries"]?.let { File(it) }
                    if (dirFile != null) {
                        if (!dirFile.exists()) {
                            val parentDir = dirFile.parentFile
                            if (parentDir == null || !parentDir.canWrite()) {
                                pathWarningValue.value =
                                    "Query directory '$dirFile' cannot be created. Please change value or permissions"
                                return@TextButton
                            }
                        }
                    }
                    keys.forEachIndexed { index, key ->
                        Settings.map[key] = textFields[index].value
                    }
                    rootScope.launch(Dispatchers.IO) {
                        Settings.save()
                        onCloseClicked()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { CoroutineScope(Dispatchers.IO).launch { onCloseClicked() } }
            ) {
                Text("Dismiss")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                keys.forEachIndexed { index, key ->
                    if (key !in advancedKeys) {
                        Row {
                            Tooltip(text = Settings.helpText[key] ?: key, Modifier.align(Alignment.CenterVertically)) {
                                Icon(
                                    painterResource(Icons.Help),
                                    contentDescription = "Query Settings",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                        .align(Alignment.CenterVertically),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            TextField(
                                value = textFields[index].value,
                                onValueChange = { textFields[index].value = it },
                                label = { Text(key) },
                                placeholder = { Text(Settings.map[key] ?: "") }
                            )
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
