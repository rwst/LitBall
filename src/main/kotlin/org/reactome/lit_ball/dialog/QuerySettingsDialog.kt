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
import org.reactome.lit_ball.common.Query
import org.reactome.lit_ball.common.QuerySetting
import org.reactome.lit_ball.common.QueryStatus
import kotlin.reflect.KSuspendFunction0

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QuerySettingsDialog(
    item: Query,
    rootScope: CoroutineScope,
    onCloseClicked: KSuspendFunction0<Unit>,
) {
    item.setting = item.setting ?: QuerySetting()
    val field1Value = rememberSaveable { mutableStateOf( item.setting?.mandatoryKeyWords?.joinToString(separator = ", ")?: "") }
    val field2Value = rememberSaveable{ mutableStateOf( item.setting?.forbiddenKeyWords?.joinToString(separator = ", ")?: "") }
    val field3Value = rememberSaveable{ mutableStateOf( item.setting?.classifier?: "") }

    AlertDialog(
        onDismissRequest = {
            rootScope.launch { (onCloseClicked)() }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    item.setting!!.mandatoryKeyWords = field1Value.value.split(",")
                        .map { it.trim() }
                        .toMutableSet()
                    item.setting!!.forbiddenKeyWords = field2Value.value.split(",")
                        .map { it.trim() }
                        .toMutableSet()
                    item.setting!!.classifier = field3Value.value
                    if (item.setting!!.mandatoryKeyWords.size > 0)
                        item.status = QueryStatus.ANNOTATED
                    println(field3Value.value)
                    rootScope.launch(Dispatchers.IO) {
                        item.saveSettings()
                    }
                    rootScope.launch { (onCloseClicked)() }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    rootScope.launch { (onCloseClicked)() }
                }
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text(
                "Edit query settings",
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                TextField(
                    value = field1Value.value,
                    onValueChange = { field1Value.value = it },
                    label = { Text("Mandatory keywords") },
                    placeholder = { Text("text1, text2, ...")}
                )
                TextField(
                    value = field2Value.value,
                    onValueChange = { field2Value.value = it },
                    label = { Text("Forbidden keywords") },
                    placeholder = { Text("text1, text2, ...")}
                )
                TextField(
                    value = field3Value.value,
                    onValueChange = { field3Value.value = it },
                    label = { Text("Classifier model name") },
                )
            }
        },
    )
}