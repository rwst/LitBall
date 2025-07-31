@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import common.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import window.components.Icons
import window.components.Tooltip
import java.io.File

@Composable
internal fun SettingsDialog(
    rootScope: CoroutineScope,
    onCloseClicked: suspend () -> Unit,
) {
    val keys = Settings.map.keys.toList()
    val advancedKeys = Settings.advancedSet
    val textFields = rememberSaveable { keys.map { key -> mutableStateOf(Settings.map[key] ?: "") } }
    val isAdvancedSettingsVisible = remember { mutableStateOf(false) }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
    val pathKey = "path-to-queries"

    Surface(
        modifier = Modifier.padding(16.dp).widthIn(max = 720.dp).heightIn(max = 600.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit settings", style = MaterialTheme.typography.h6)
            }
            Spacer(modifier = Modifier.height(16.dp))

            val listState = rememberLazyListState()
            val normalKeys = keys.filter { it !in advancedKeys }
            val advKeys = keys.filter { it in advancedKeys }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 12.dp)
                ) {
                    items(normalKeys) { key ->
                        val originalIndex = keys.indexOf(key)
                        SettingItem(key, textFields[originalIndex], key == pathKey, pathWarningValue)
                    }
                    item {
                        Text(
                            text = "Advanced Settings",
                            modifier = Modifier
                                .clickable { isAdvancedSettingsVisible.value = !isAdvancedSettingsVisible.value }
                                .padding(16.dp)
                        )
                    }
                    if (isAdvancedSettingsVisible.value) {
                        items(advKeys) { key ->
                            val originalIndex = keys.indexOf(key)
                            SettingItem(key, textFields[originalIndex], false, null)
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { CoroutineScope(Dispatchers.IO).launch { onCloseClicked() } }
                ) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        pathWarningValue.value = null
                        val pathIndex = keys.indexOf(pathKey)
                        if (pathIndex != -1) {
                            val dirFile = File(textFields[pathIndex].value)
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
            }
        }
    }
}

@Composable
private fun SettingItem(
    key: String,
    valueState: MutableState<String>,
    isPathSetting: Boolean,
    pathWarningValue: MutableState<String?>?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tooltip(
                text = Settings.helpText[key] ?: key,
                modifier = Modifier,
            ) {
                Icon(
                    painterResource(Icons.Help),
                    contentDescription = "Setting Help",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            TextField(
                value = valueState.value,
                onValueChange = { valueState.value = it },
                label = { Text(key) },
                placeholder = { Text(Settings.map[key] ?: "") },
                modifier = Modifier.weight(1f)
            )
            if (isPathSetting) {
                pathWarningValue?.value?.also {
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        it,
                        color = Color.Red,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}