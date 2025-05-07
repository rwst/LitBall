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
import common.ArticleType
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
    data class QueryDialogState(
        val copyFrom: String = "",
        val queryType: Int = QueryType.SUPERVISED_SNOWBALLING.ordinal,
        val field: String = "",
        val name: String = "",
        val pubYear: String = "",
        val flagChecked: BooleanArray = BooleanArray(ArticleType.entries.size) { true },
        val check: Boolean = true,
        val nameCheck: Boolean = true,
        val typeWarning: String? = null,
        val pathWarning: String? = null,
        val doiWarning: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as QueryDialogState

            if (queryType != other.queryType) return false
            if (check != other.check) return false
            if (nameCheck != other.nameCheck) return false
            if (copyFrom != other.copyFrom) return false
            if (field != other.field) return false
            if (name != other.name) return false
            if (pubYear != other.pubYear) return false
            if (!flagChecked.contentEquals(other.flagChecked)) return false
            if (typeWarning != other.typeWarning) return false
            if (pathWarning != other.pathWarning) return false
            if (doiWarning != other.doiWarning) return false

            return true
        }

        override fun hashCode(): Int {
            var result = queryType
            result = 31 * result + check.hashCode()
            result = 31 * result + nameCheck.hashCode()
            result = 31 * result + copyFrom.hashCode()
            result = 31 * result + field.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + pubYear.hashCode()
            result = 31 * result + flagChecked.contentHashCode()
            result = 31 * result + (typeWarning?.hashCode() ?: 0)
            result = 31 * result + (pathWarning?.hashCode() ?: 0)
            result = 31 * result + (doiWarning?.hashCode() ?: 0)
            return result
        }
    }

    val state = rememberSaveable { mutableStateOf(QueryDialogState()) }
    val queryPath = Settings.map["path-to-queries"] ?: throw Exception("Path to queries not set")

    fun setState(update: QueryDialogState.() -> QueryDialogState) {
        state.value = state.value.update()
    }

    AlertDialog(
        onDismissRequest = { onCloseClicked() },
        confirmButton = {
            TextButton(
                onClick = {
                    val refs = state.value.field.split("\n")
                        .map { it.trim().transformDOI() }
                        .filter { it.isNotBlank() }
                    val name = state.value.name.trim()
                    setState { copy(pathWarning = null, doiWarning = null) }

                    if (File(queryPath).exists() && !Path(queryPath).isWritable()) {
                        setState { copy(pathWarning = "Query directory is not writable") }
                        return@TextButton
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
                                            (dois.isNotEmpty() && dois.none { !it.startsWith("10.") })),
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
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = { onCloseClicked() }) {
                Text("Dismiss")
            }
        },
        title = {
            Text("Create new query")
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                queryCopyFromComponent(mutableStateOf(state.value.copyFrom))
                Spacer(modifier = Modifier.height(8.dp))
                queryTypeComponent(mutableStateOf(state.value.queryType), mutableStateOf(state.value.typeWarning))
                Spacer(modifier = Modifier.height(8.dp))
                queryNameComponent(mutableStateOf(state.value.name), mutableStateOf(state.value.pathWarning))

                if (state.value.copyFrom.isNotBlank()) {
                    setState {
                        copy(name = generateSequence(1) { it + 1 }
                            .map { "${copyFrom}-$it" }
                            .first { it !in QueryList.list.map { query -> query.name } }
                        )
                    }
                }

                if (state.value.queryType > 0) {
                    queryPaperIdsComponent(
                        mutableStateOf(state.value.field),
                        mutableStateOf(state.value.pathWarning),
                        mutableStateOf(state.value.doiWarning)
                    )
                } else {
                    queryPublicationDateComponent(mutableStateOf(state.value.pubYear))
                    Spacer(modifier = Modifier.height(24.dp))
                    queryArticleTypeComponent(mutableStateOf(state.value.flagChecked))
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