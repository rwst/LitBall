@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.QuerySetting
import org.reactome.lit_ball.common.LitBallQuery
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
    val field3Value = rememberSaveable { mutableStateOf(item.setting?.pubDateFilterFrom ?: "") }
    val field4Value = rememberSaveable { mutableStateOf(item.setting?.pubDateFilterTo ?: "") }
    val field5Value = rememberSaveable { mutableStateOf(item.setting?.classifier ?: "") }
    val field6Value =
        rememberSaveable { mutableStateOf(item.setting?.annotationClasses?.joinToString(separator = ", ") ?: "") }

    fun closeDialog() {
        rootScope.launch { onCloseClicked() }
    }

    AlertDialog(
        onDismissRequest = { closeDialog() },
        confirmButton = {
            TextButton(
                onClick = {
                    item.setting!!.mandatoryKeyWords = field1Value.value.splitToSet(",")
                    item.setting!!.forbiddenKeyWords = field2Value.value.splitToSet(",")
                    item.setting!!.pubDateFilterFrom = field3Value.value.trim()
                    item.setting!!.pubDateFilterTo = field4Value.value.trim()
                    item.setting!!.classifier = field5Value.value.trim()
                    item.setting!!.classifier = field5Value.value.trim()
                    item.setting!!.annotationClasses = field6Value.value.splitToSet(",")
                    rootScope.launch(Dispatchers.IO) {
                        item.saveSettings()
                    }
                    closeDialog()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { closeDialog() }
            ) {
                Text("Dismiss")
            }
        },
        title = { Text("Edit query settings") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                TextFieldWithPlaceholder(field1Value, "Mandatory keywords", "text1, text2, ...")
                TextFieldWithPlaceholder(field2Value, "Forbidden keywords", "text1, text2, ...")
                TextFieldWithPlaceholder(field3Value, "From (Date)", "1949-12-31")
                TextFieldWithoutPlaceholder(field4Value, "To (Date)")
                TextFieldWithoutPlaceholder(field5Value, "Classifier model name")
                TextFieldWithPlaceholder(field6Value, "Annotation classes", "text1, text2, ...")
            }
        },
    )
}

@Composable
fun TextFieldWithPlaceholder(
    value: MutableState<String>,
    label: String,
    placeholder: String,
) {
    TextField(
        value = value.value,
        onValueChange = { value.value = it },
        label = { Text(label) },
        placeholder = { Text(placeholder) }
    )
}

@Composable
fun TextFieldWithoutPlaceholder(
    value: MutableState<String>,
    label: String,
) {
    TextField(
        value = value.value,
        onValueChange = { value.value = it },
        label = { Text(label) },
    )
}