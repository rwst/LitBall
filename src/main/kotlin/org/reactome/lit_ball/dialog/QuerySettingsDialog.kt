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
import org.reactome.lit_ball.common.LitBallQuery
import org.reactome.lit_ball.common.QuerySetting
import org.reactome.lit_ball.util.StringPatternMatcher
import org.reactome.lit_ball.util.splitToSet

@Composable
fun QuerySettingsDialog(
    item: LitBallQuery,
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    item.setting = item.setting ?: QuerySetting()
    val field1Value =
        rememberSaveable { mutableStateOf(item.setting?.mandatoryKeyWords?.joinToString(separator = ", ") ?: "") }
    val field2Value =
        rememberSaveable { mutableStateOf(item.setting?.forbiddenKeyWords?.joinToString(separator = ", ") ?: "") }
    val field3Value = rememberSaveable { mutableStateOf(item.setting?.classifier ?: "") }
    val field4Value =
        rememberSaveable { mutableStateOf(item.setting?.annotationClasses?.joinToString(separator = ", ") ?: "") }
    val keyword1WarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }
    val keyword2WarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }

    AlertDialog(
        onDismissRequest = {
            rootScope.launch { (onCloseClicked)() }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyword1WarningValue.value = null
                    if (!StringPatternMatcher.validateSetting(field1Value.value)) {
                        keyword1WarningValue.value = "Invalid expression"
                        return@TextButton
                    }
                    keyword2WarningValue.value = null
                    if (!StringPatternMatcher.validateSetting(field2Value.value)) {
                        keyword2WarningValue.value = "Invalid expression"
                        return@TextButton
                    }

                    item.setting!!.mandatoryKeyWords = StringPatternMatcher.patternSettingFrom(field1Value.value)
                    item.setting!!.forbiddenKeyWords = StringPatternMatcher.patternSettingFrom(field2Value.value)
                    item.setting!!.classifier = field3Value.value.trim()
                    item.setting!!.annotationClasses = field4Value.value.splitToSet(",")
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
                Row {
                    TextField(
                        value = field1Value.value,
                        onValueChange = {
                            field1Value.value = it
                            keyword1WarningValue.value = null
                                        },
                        label = { Text("Mandatory keywords / expression") },
                        placeholder = { Text("") }
                    )
                    keyword1WarningValue.value?.also {
                        Text(it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                        )
                    }
                }
                Row {
                    TextField(
                        value = field2Value.value,
                        onValueChange = {
                            field2Value.value = it
                            keyword2WarningValue.value = null
                                        },
                        label = { Text("Forbidden keywords / expression") },
                        placeholder = { Text("") }
                    )
                    keyword2WarningValue.value?.also {
                        Text(it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                        )
                    }
                }
                TextField(
                    value = field3Value.value,
                    onValueChange = { field3Value.value = it },
                    label = { Text("Classifier model name") },
                )
                TextField(
                    value = field4Value.value,
                    onValueChange = { field4Value.value = it },
                    label = { Text("Annotation classes") },
                    placeholder = { Text("text1, text2, ...") }
                )
            }
        },
    )
}

