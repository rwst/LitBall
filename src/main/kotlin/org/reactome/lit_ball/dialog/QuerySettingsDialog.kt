@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import common.LitBallQuery
import common.QueryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun QuerySettingsDialog(
    item: LitBallQuery,
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val entries = mutableListOf<QuerySettingEntry>()
    when (item.type) {
        QueryType.EXPRESSION_SEARCH -> {
            entries.add(MandatoryKeywordsEntry(item.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(item.setting.forbiddenKeyWords.toList()))
            entries.add(PublicationDateSettingEntry(item.setting.pubDate))
            entries.add(ArticleTypeSettingEntry(item.setting.pubType))
        }

        QueryType.SIMILARITY_SEARCH -> {

        }

        else -> {
            entries.add(MandatoryKeywordsEntry(item.setting.mandatoryKeyWords.toList()))
            entries.add(ForbiddenKeywordsEntry(item.setting.forbiddenKeyWords.toList()))
            entries.add(ClassifierEntry(item.setting.classifier))
            entries.add(AnnotationClassesEntry(item.setting.annotationClasses.toList()))
        }
    }

    QuerySettingsDialog(
        item = item,
        rootScope = rootScope,
        onCloseClicked = onCloseClicked,
        settingEntries = entries
    )
//    val field1Value =
//        rememberSaveable { mutableStateOf(item.setting.mandatoryKeyWords.joinToString(separator = ", ")) }
//    val field2Value =
//        rememberSaveable { mutableStateOf(item.setting.forbiddenKeyWords.joinToString(separator = ", ")) }
//    val field3Value = rememberSaveable { mutableStateOf(item.setting.classifier) }
//    val field4Value =
//        rememberSaveable { mutableStateOf(item.setting.annotationClasses.joinToString(separator = ", ")) }
//    val keyword1WarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
//    val keyword2WarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
//    val typeState =
//        rememberSaveable { mutableStateOf(
//            ArticleTypeDialogState(flagChecked = typeStringsToBoolArray(item.setting.pubType))) }
//    val dateState =
//        rememberSaveable { mutableStateOf(PublicationDateDialogState(pubYear = item.setting.pubDate)) }
//
//    AlertDialog(
//        onDismissRequest = {
//            rootScope.launch { (onCloseClicked)() }
//        },
//        confirmButton = {
//            TextButton(
//                onClick = {
//                    keyword1WarningValue.value = null
//                    if (!StringPatternMatcher.validateSetting(field1Value.value)) {
//                        keyword1WarningValue.value = "Invalid expression"
//                        return@TextButton
//                    }
//                    keyword2WarningValue.value = null
//                    if (!StringPatternMatcher.validateSetting(field2Value.value)) {
//                        keyword2WarningValue.value = "Invalid expression"
//                        return@TextButton
//                    }
//
//                    item.setting.mandatoryKeyWords.clear()
//                    item.setting.mandatoryKeyWords.addAll(StringPatternMatcher.patternSettingFrom(field1Value.value))
//                    item.setting.forbiddenKeyWords.clear()
//                    item.setting.forbiddenKeyWords.addAll(StringPatternMatcher.patternSettingFrom(field2Value.value))
//                    item.setting.classifier = field3Value.value.trim()
//                    item.setting.annotationClasses.clear()
//                    item.setting.annotationClasses.addAll(field4Value.value.splitToSet(","))
//                    item.setting.type = item.type
//                    item.setting.pubType.clear()
//                    item.setting.pubType.addAll(boolArrayToTypeStrings( typeState.value.flagChecked))
//                    item.setting.pubDate = dateState.value.pubYear.trim()
//
//                    rootScope.launch(Dispatchers.IO) {
//                        item.saveSettings()
//                    }
//                    rootScope.launch { (onCloseClicked)() }
//                }
//            ) {
//                Text("Confirm")
//            }
//        },
//        dismissButton = {
//            TextButton(
//                onClick = {
//                    rootScope.launch { (onCloseClicked)() }
//                }
//            ) {
//                Text("Dismiss")
//            }
//        },
//        title = {
//            Text(
//                "Edit query settings",
//            )
//        },
//        text = {
//            Column(horizontalAlignment = Alignment.Start) {
//                Row {
//                    Tooltip(
//                        text = """
//                        Enter either
//                        1. keywords/phrases separated by comma, no wildcards, or
//                        2. logical expression of keywords/phrases starting with
//                           open parenthesis and containing operators "or", "and",
//                           "not", wildcard "*", and matched parentheses.
//                        In both cases keyphrases are matched to words in title,
//                        abstract, and TLDR for a positive match.
//                    """.trimIndent(),
//                        Modifier.align(Alignment.CenterVertically)
//                    ) {
//                        Icon(
//                            painterResource(Icons.Help),
//                            contentDescription = "Query Settings",
//                            tint = Color.Gray,
//                            modifier = Modifier.size(20.dp)
//                                .align(Alignment.CenterVertically),
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(14.dp))
//                    TextField(
//                        value = field1Value.value,
//                        onValueChange = {
//                            field1Value.value = it
//                            keyword1WarningValue.value = null
//                        },
//                        label = { Text("Mandatory keywords / expression") },
//                        placeholder = { Text("") }
//                    )
//                    Spacer(modifier = Modifier.width(14.dp))
//                    keyword1WarningValue.value?.also {
//                        Text(
//                            it,
//                            color = Color.Red,
//                            modifier = Modifier
//                                .align(Alignment.CenterVertically)
//                                .padding(start = 24.dp)
//                        )
//                    }
//                }
//                Row {
//                    Tooltip(
//                        text = """
//                        Optionally enter either
//                        1. keywords/phrases separated by comma, no wildcards, or
//                        2. logical expression of keywords/phrases starting with
//                           open parenthesis and containing operators "or", "and",
//                           "not", wildcard "*", and matched parentheses.
//                        In both cases keyphrases are matched to words in title,
//                        for a negative match.
//                    """.trimIndent(),
//                        Modifier.align(Alignment.CenterVertically)
//                    ) {
//                        Icon(
//                            painterResource(Icons.Help),
//                            contentDescription = "Query Settings",
//                            tint = Color.Gray,
//                            modifier = Modifier.size(20.dp)
//                                .align(Alignment.CenterVertically),
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(14.dp))
//                    TextField(
//                        value = field2Value.value,
//                        onValueChange = {
//                            field2Value.value = it
//                            keyword2WarningValue.value = null
//                        },
//                        label = { Text("Forbidden keywords / expression") },
//                        placeholder = { Text("") }
//                    )
//                    Spacer(modifier = Modifier.width(14.dp))
//                    keyword2WarningValue.value?.also {
//                        Text(
//                            it,
//                            color = Color.Red,
//                            modifier = Modifier
//                                .align(Alignment.CenterVertically)
//                                .padding(start = 24.dp)
//                        )
//                    }
//                }
//                Row {
//                    Tooltip(
//                        text = """
//                        On Linux, if the YDF package is installed, this
//                        is the name of the model that will be used for
//                        automated filtering in the Supervised Filtering
//                        screen.
//                    """.trimIndent(),
//                        Modifier.align(Alignment.CenterVertically)
//                    ) {
//                        Icon(
//                            painterResource(Icons.Help),
//                            contentDescription = "Query Settings",
//                            tint = Color.Gray,
//                            modifier = Modifier.size(20.dp)
//                                .align(Alignment.CenterVertically),
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(14.dp))
//                    TextField(
//                        value = field3Value.value,
//                        onValueChange = { field3Value.value = it },
//                        label = { Text("Classifier model name") },
//                    )
//                    Spacer(modifier = Modifier.width(14.dp))
//                }
//                Row {
//                    Tooltip(
//                        text = """
//                            If this field contains words separated
//                            by comma, the same words will appear as
//                            clickboxes in the Annotation Screen on every
//                            paper. Tagged papers will, on export, be
//                            sorted in tag-associated CSV files inside
//                            the query directory.
//                    """.trimIndent(),
//                        Modifier.align(Alignment.CenterVertically)
//                    ) {
//                        Icon(
//                            painterResource(Icons.Help),
//                            contentDescription = "Query Settings",
//                            tint = Color.Gray,
//                            modifier = Modifier.size(20.dp)
//                                .align(Alignment.CenterVertically),
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(14.dp))
//                    TextField(
//                        value = field4Value.value,
//                        onValueChange = { field4Value.value = it },
//                        label = { Text("Annotation classes") },
//                        placeholder = { Text("text1, text2, ...") }
//                    )
//                    Spacer(modifier = Modifier.width(14.dp))
//
//                }
//                if (item.type == QueryType.EXPRESSION_SEARCH) {
//                    Spacer(modifier = Modifier.height(14.dp))
//                    queryPublicationDateComponent(dateState)
//                    Spacer(modifier = Modifier.height(14.dp))
//                    queryArticleTypeComponent(typeState)
//                }
//            }
//        },
//    )
}

@Composable
fun QuerySettingsDialog(
    item: LitBallQuery,
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
    settingEntries: List<QuerySettingEntry> // Accepts a list of setting entries
) {
    AlertDialog(
        onDismissRequest = {
            rootScope.launch { onCloseClicked() }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var allValid = true
                    for (entry in settingEntries) {
                        if (!entry.validate()) {
                            allValid = false
                        }
                    }

                    if (allValid) {
                        settingEntries.forEach { entry ->
                            entry.applyTo(item.setting)
                        }
                        item.setting.type = item.type // Preserve original logic

                        rootScope.launch(Dispatchers.IO) {
                            item.saveSettings()
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
                settingEntries.forEach { entry ->
                    entry.View()
                }
            }
        },
    )
}