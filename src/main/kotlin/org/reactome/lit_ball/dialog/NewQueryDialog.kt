@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import common.QueryList
import common.QueryType
import common.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val refs = state.value.field.split("\n")
            .map { it.trim().transformDOI() }
            .filter { it.isNotBlank() }
        val name = state.value.name.trim()
        setState { copy(pathWarning = null, doiWarning = null) }

        if (File(queryPath).exists() && !Path(queryPath).isWritable()) {
            setState { copy(pathWarning = "Query directory is not writable") }
            return
        }
        var dois: List<String> = emptyList()
        rootScope.launch(Dispatchers.IO) {
            setState { copy(doiWarning = "Retrieving DOIs of ${refs.size} references...") }
            try {
                dois = fillDOIs(refs)
                setState {
                    copy(
                        doiWarning = null,
                        field = dois.joinToString("\n")
                    )
                }
                if (dois.any { !it.startsWith("10.") }) {
                    setState { copy(doiWarning = "Could not convert all entries to DOI. Please replace or remove.") }
                }
            } catch (e: Exception) {
                setState { copy(doiWarning = "Error: ${e.message}") }
            }
        }

        setState {
            copy(
                check = name.isNotEmpty() &&
                        (queryType == QueryType.EXPRESSION_SEARCH.ordinal ||
                                (dois.isNotEmpty() && dois.all { it.startsWith("10.") })),
                nameCheck = name !in QueryList.list.map { it.name }
            )
        }

        if (state.value.check && state.value.nameCheck) {
            rootScope.launch(Dispatchers.IO) {
                QueryList.addNewItem(
                    QueryType.entries[state.value.queryType],
                    name,
                    dois.toSet(),
                    Pair(state.value.pubYear, state.value.flagChecked)
                )
            }
            onCloseClicked()
        }
    }

    fun generateUniqueQueryName() {
        if (state.value.copyFrom.isNotBlank()) {
            setState {
                copy(name = generateSequence(1) { it + 1 }
                    .map { "${copyFrom}-$it" }
                    .first { it !in QueryList.list.map { query -> query.name } }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onCloseClicked() },
        confirmButton = {
            TextButton(
                onClick = { processConfirmation() }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onCloseClicked() }
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text("Create new query")
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                queryCopyFromComponent(state)
                Spacer(modifier = Modifier.height(8.dp))
                queryTypeComponent(state)
                Spacer(modifier = Modifier.height(8.dp))
                queryNameComponent(state)

                generateUniqueQueryName()

                if (state.value.queryType > 0) {
                    queryPaperIdsComponent(state)
                } else {
                    queryPublicationDateComponent(state)
                    Spacer(modifier = Modifier.height(24.dp))
                    queryArticleTypeComponent(state)
                }

                if (!state.value.check)
                    Text("Please fill in the text fields.")
                if (!state.value.nameCheck)
                    Text("Query name already exists in directory.")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(28.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    )
}