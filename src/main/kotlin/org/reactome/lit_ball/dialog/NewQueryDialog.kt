@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import common.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import util.CantHappenException
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.isWritable


@Composable
fun NewQueryDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val state = rememberSaveable { mutableStateOf(QueryDialogState()) }
    val queryPath = Settings.map["path-to-queries"] ?: throw Exception("Path to queries not set")

    fun setState(update: QueryDialogState.() -> QueryDialogState) {
        state.value = state.value.update()
    }

    fun processConfirmation() {
        val refs = state.value.paperIds.split("\n")
            .map { it.trim().transformDOI() }
            .filter { it.isNotBlank() }
        val name = state.value.name.trim()
        setState { copy(pathWarning = null) }

        if (File(queryPath).exists() && !Path(queryPath).isWritable()) {
            setState { copy(pathWarning = "Query directory is not writable") }
            return
        }
        setState { copy(doiWarning = "Retrieving PaperIds of ${refs.size} PMID references...") }
        var paperIds: List<String>
        rootScope.launch(Dispatchers.IO) {
            try {
                paperIds = fillDOIs(refs)
                setState {
                    copy(
                        doiWarning = null,
                        paperIds = paperIds.joinToString("\n")
                    )
                }
                if (paperIds.any { !it.startsWith("10.") && !it.startsWith("s2:") }) {
                    setState { copy(doiWarning = "Could not convert all entries to DOI. Please replace or remove PMID numbers.") }
                }
            } catch (e: Exception) {
                setState { copy(doiWarning = "Error: ${e.message}") }
            }
        }
        val newPaperIds = state.value.paperIds.split("\n")
            .map { it.trim().transformDOI() }
            .filter { it.isNotBlank() }

        setState {
            copy(
                check = name.isNotEmpty() &&
                        (queryType == QueryType.EXPRESSION_SEARCH.ordinal ||
                                (newPaperIds.isNotEmpty() && newPaperIds.all { it.startsWith("10.") || it.startsWith("s2:") })),
                nameCheck = name !in QueryList.list.map { it.name }
            )
        }

        if (state.value.check && state.value.nameCheck) {
            rootScope.launch(Dispatchers.IO) {
                QueryList.addNewItem(
                    QueryType.entries[state.value.queryType],
                    name,
                    newPaperIds.toSet(),
                    Pair(state.value.pubYear, state.value.flagChecked),
                    state.value.mandatoryKeyWords,
                    state.value.forbiddenKeyWords,
                )
                onCloseClicked()
            }
        }
    }

    fun processCopyFrom() {
        val fromName = state.value.copyFrom
        val fromQuery = QueryList.list.find { it.name == fromName } ?: throw CantHappenException()

        setState {
            copy(name = generateSequence(1) { it + 1 }
                .map { "${fromName}-$it" }
                .first { it !in QueryList.list.map { query -> query.name } }
            )
        }
        if (state.value.queryType != QueryType.EXPRESSION_SEARCH.ordinal) {
            val newField = getDOIs(getQueryDir(fromName), FileType.ACCEPTED)
                .joinToString("\n")
            setState { copy(paperIds = newField) }
        } else {
            if (fromQuery.type == QueryType.EXPRESSION_SEARCH) {
                setState {
                    copy(
                        flagChecked = typeStringsToBoolArray(fromQuery.setting.pubType),
                        pubYear = fromQuery.setting.pubDate,
                    )
                }
            }
        }
        if (fromQuery.type == QueryType.EXPRESSION_SEARCH &&
            state.value.queryType != QueryType.SIMILARITY_SEARCH.ordinal
        ) {
            setState {
                copy(
                    mandatoryKeyWords = fromQuery.setting.mandatoryKeyWords.toList(),
                    forbiddenKeyWords = fromQuery.setting.forbiddenKeyWords.toList(),
                )
            }
        }
    }

    Dialog(onDismissRequest = { rootScope.launch { onCloseClicked() } }) {
        Surface(
            modifier = Modifier
                .width(700.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LaunchedEffect(state.value.copyFrom) {
                    if (state.value.copyFrom.isNotBlank()) {
                        processCopyFrom()
                    }
                }
                Text(
                    text = "Create new query",
                    style = MaterialTheme.typography.titleMedium
                )
                queryTypeComponent(state)
                queryCopyFromComponent(state)
                queryNameComponent(state)

                if (state.value.queryType > 0) {
                    queryPaperIdsComponent(state)
                } else {
                    queryPublicationDateComponent(state)
                    Spacer(modifier = Modifier.height(8.dp))
                    queryArticleTypeComponent(state)
                }

                if (!state.value.check)
                    Text(
                        "Please fill in the text fields.",
                        color = Color.Red,
                    )
                if (!state.value.nameCheck)
                    Text(
                        "Query name already exists in directory.",
                        color = Color.Red,
                    )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onCloseClicked() }
                    ) {
                        Text("Dismiss")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { processConfirmation() }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}