@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    when (query.type) {
        QueryType.EXPRESSION_SEARCH -> {
            entries.add(MandatoryKeywordsEntry(query.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(query.setting.forbiddenKeyWords.toList()))
            entries.add(PublicationDateSettingEntry(query.setting.pubDate))
            entries.add(ArticleTypeSettingEntry(query.setting.pubType))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
        }

        QueryType.SIMILARITY_SEARCH -> {
            entries.add(PaperIdsSettingEntry(""))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
        }

        else -> {
            entries.add(MandatoryKeywordsEntry(query.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(query.setting.forbiddenKeyWords.toList()))
            entries.add(ClassifierEntry(query.setting.classifier))
            entries.add(AnnotationClassesEntry(query.setting.annotationClasses.toList()))
            entries.add(PaperIdsSettingEntry(""))
        }
    }

    AlertDialog(
        onDismissRequest = {
            rootScope.launch { onCloseClicked() }
        },
        confirmButton = {
            TextButton(
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
        },
        dismissButton = {
            TextButton(
                onClick = {
                    rootScope.launch { onCloseClicked() }
                }
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text("Edit query settings")
        },
        text = {
            Column(
                modifier = Modifier.widthIn(min = 500.dp, max = 700.dp), // Control dialog width
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Spacing between entries
            ) {
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
            }
        },
    )
}