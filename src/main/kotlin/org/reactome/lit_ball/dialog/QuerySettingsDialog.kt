@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import common.LitBallQuery
import common.QueryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun QuerySettingsDialog(
    query: LitBallQuery,
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val entries = mutableListOf<QuerySettingEntry>()
    val warning = mutableStateOf<String?>(null)
    var height = 650.dp
    when (query.type) {
        QueryType.EXPRESSION_SEARCH -> {
            entries.add(MandatoryKeywordsEntry(query.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(query.setting.forbiddenKeyWords.toList()))
            entries.add(PublicationDateSettingEntry(query.setting.pubDate))
            entries.add(ArticleTypeSettingEntry(query.setting.pubType))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
        }

        QueryType.SIMILARITY_SEARCH -> {
            entries.add(PaperIdsSettingEntry(query.acceptedSet))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
            height = 500.dp
        }

        else -> {
            entries.add(MandatoryKeywordsEntry(query.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(query.setting.forbiddenKeyWords.toList()))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
            entries.add(PaperIdsSettingEntry(query.acceptedSet))
        }
    }

    Dialog(onDismissRequest = { rootScope.launch { onCloseClicked() } }) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .height(height),
            shape = MaterialTheme.shapes.medium, // Standard dialog shape
            elevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 500.dp, max = 700.dp)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp), // Spacing between entries
            ) {
                Text(
                    text = "Edit query settings",
                    style = MaterialTheme.typography.titleMedium
                )
                entries.forEach { entry ->
                    entry.View()
                }
                warning.value?.also {
                    Text(
                        warning.value!!,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(start = 24.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            rootScope.launch { onCloseClicked() }
                        }
                    ) {
                        Text("Dismiss")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val allValid = entries.all { it.validate(warning) }
                            if (allValid) {
                                warning.value = null
                                entries.forEach { entry ->
                                    entry.applyTo(query.setting)
                                }
                                rootScope.launch(Dispatchers.IO) {
                                    query.saveSettings()
                                }
                                rootScope.launch { onCloseClicked() }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}