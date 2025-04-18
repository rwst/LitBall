@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import common.ArticleType
import common.QueryList
import common.QueryType
import common.Settings
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.isWritable


@Composable
fun NewQueryDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val copyFromValue = rememberSaveable { mutableStateOf("") }
    val typeValue = rememberSaveable { mutableStateOf(2) }
    val fieldValue = rememberSaveable { mutableStateOf("") }
    val nameValue = rememberSaveable { mutableStateOf("") }
    val pubYearValue = rememberSaveable { mutableStateOf("") }
    val flagCheckedValue = rememberSaveable { mutableStateOf(BooleanArray(ArticleType.entries.size) { true }) }
    val checkValue = rememberSaveable { mutableStateOf(true) }
    val nameCheckValue = rememberSaveable { mutableStateOf(true) }
    val typeWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
    val doiWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null) }
    rememberSaveable { mutableStateOf(null) }
    val queryPath = Settings.map["path-to-queries"] ?: ""

    AlertDialog(
        onDismissRequest = { (onCloseClicked)() },
        confirmButton = {
            TextButton(
                onClick = {
                    val refs = fieldValue.value.split("\n")
                        .map { it.trim().transformDOI() }
                        .filter { it.isNotBlank() }
                    val name = nameValue.value.trim()
                    pathWarningValue.value = null
                    doiWarningValue.value = null

                    if (File(queryPath).exists() && !Path(queryPath).isWritable()) {
                        pathWarningValue.value = "Query directory is not writable"
                        return@TextButton
                    }
                    var dois: List<String>
                    runBlocking {
                        dois = fillDOIs(refs)
                    }
                    fieldValue.value = dois.joinToString("\n")
                    if (dois.any { !it.startsWith("10.") }) {
                        doiWarningValue.value = "Could not convert all entries to DOI. Please replace or remove."
                        return@TextButton
                    }
                    checkValue.value = name.isNotEmpty() && (typeValue.value == 0 || dois.isNotEmpty())
                    nameCheckValue.value = name !in QueryList.list.map { it.name }
                    if (checkValue.value && nameCheckValue.value) {
                        rootScope.launch(Dispatchers.IO) {
                            QueryList.addNewItem(
                                QueryType.entries[typeValue.value],
                                name,
                                dois.toSet(),
                                Pair(pubYearValue.value, flagCheckedValue.value)
                            )
                        }
                        (onCloseClicked)()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = { (onCloseClicked)() } ) {
                Text("Dismiss")
            }
        },
        title = {
            Text("Create new query")
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                queryCopyFromComponent(copyFromValue)
                Spacer(modifier = Modifier.height(8.dp))
                queryTypeComponent(typeValue, typeWarningValue)
                Spacer(modifier = Modifier.height(8.dp))
                queryNameComponent(nameValue, pathWarningValue)

                if (copyFromValue.value.isNotBlank()) {
                    nameValue.value = generateSequence(1) { it + 1 }
                        .map { "${copyFromValue.value}-$it" }
                        .first { it !in QueryList.list.map { query -> query.name } }
                }

                if (typeValue.value > 0) {
                    queryPaperIdsComponent(fieldValue, pathWarningValue, doiWarningValue)
                } else {
                    queryPublicationDateComponent(pubYearValue)
                    Spacer(modifier = Modifier.height(24.dp))
                    queryArticleTypeComponent(flagCheckedValue)
                }

                if (!checkValue.value)
                    Text("Please fill in the text fields.")
                if (!nameCheckValue.value)
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
