@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.reactome.lit_ball.common.Qtype
import org.reactome.lit_ball.common.QueryList
import org.reactome.lit_ball.common.Settings
import org.reactome.lit_ball.service.getDOIsforPMIDs
import org.reactome.lit_ball.window.components.Icons
import org.reactome.lit_ball.window.components.Tooltip
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.isWritable


@Composable
fun NewItemDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
) {
    val typeValue = rememberSaveable { mutableStateOf(2) }
    val fieldValue = rememberSaveable { mutableStateOf("") }
    val nameValue = rememberSaveable { mutableStateOf("") }
    val checkValue = rememberSaveable { mutableStateOf(true) }
    val nameCheckValue = rememberSaveable { mutableStateOf(true) }
    val typeWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }
    val pathWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }
    val doiWarningValue: MutableState<String?> = rememberSaveable { mutableStateOf(null)  }

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

                    val queryPath = Settings.map["path-to-queries"] ?: ""
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
                            QueryList.addNewItem(typeValue.value, name, dois.toSet())
                        }
                        (onCloseClicked)()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { (onCloseClicked)() },
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text(
                "Create new query",
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Row {
                    Tooltip(text = """
                        Available query types are:
                        1. Simple expression search: your positive and negative
                           keyphrases/expressions are sent to Semantic Scholar
                           for a search over the whole graph. Starting DOIs are
                           ignored.
                        2. Snowballing with automated keyphrase/expression
                           filtering. No supervised filtering (all matches are
                           accepted).
                        3. (default) Snowballing with automated and supervised
                           filtering.
                    """.trimIndent(),
                        Modifier.align(Alignment.CenterVertically)) {
                        Icon(
                            painterResource(Icons.Help),
                            contentDescription = "Query Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                                .align(Alignment.CenterVertically),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    RadioButtonOptions(
                        Qtype.entries.map { it.pretty },
                        typeValue.value,
                        onOptionSelected = { btn ->
                            if (btn == 0 && Settings.map["S2-API-key"].isNullOrEmpty()) {
                                typeWarningValue.value = "S2 API key needed"
                            }
                            else {
                                typeWarningValue.value = null
                            }
                            typeValue.value = btn
                        }
                    )
                    typeWarningValue.value?.also {
                        Text(
                            it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Tooltip(text = """
                        Enter name of query. The string is cut off
                        at the first '/' (slash) character.
                    """.trimIndent(),
                        Modifier.align(Alignment.CenterVertically)) {
                        Icon(
                            painterResource(Icons.Help),
                            contentDescription = "Query Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                                .align(Alignment.CenterVertically),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    TextField(
                        value = nameValue.value,
                        onValueChange = {
                            nameValue.value = it.substringBefore("/")
                                        },
                        label = { Text("Query name") },
                    )
                    pathWarningValue.value?.also {
                        Text(
                            it,
                            color = Color.Red,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 24.dp)
                        )
                    }
                }
                if (typeValue.value > 0) {
                    Row {
                        Tooltip(
                            text = """
                            Input one DOI/PMID per line. It is not necessary to manually trim
                            the DOI strings. LitBall will automatically chop off everything
                            before the “10.” part, so simply copypasting a DOI link will be
                            handled. PMID links will be stripped to just the number""".trimIndent(),
                            Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                painterResource(Icons.Help),
                                contentDescription = "Query Settings",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                                    .align(Alignment.CenterVertically),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        TextField(
                            value = fieldValue.value,
                            onValueChange = {
                                fieldValue.value = it.split('\n').joinToString("\n") { s -> s.trim().transformDOI() }
                                pathWarningValue.value = null
                            },
                            label = { Text("Core DOIs/PMIDs (one per line)") },
                            placeholder = { Text("10.XYZ/ABC\n12345678") }
                        )
                        doiWarningValue.value?.also {
                            Text(
                                it,
                                color = Color.Red,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 24.dp)
                            )
                        }
                    }
                }
                if (!checkValue.value)
                    Text("Please fill in the text fields.")
                if (!nameCheckValue.value)
                    Text("Query name already exists in directory.")
            }
        },
    )
}

val pmidRegex = "^pmid:*\\h*".toRegex(RegexOption.IGNORE_CASE)
val slashSuffix = "/$".toRegex()
private fun String.transformDOI(): String {
    var s = this.uppercase()
    if (s.startsWith("HTTP"))
        s = URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
    s = s.replaceBefore("10.", "")
    s = s.replace(pmidRegex, "").replace("HTTPS://PUBMED.NCBI.NLM.NIH.GOV/", "")
    s = s.replace(slashSuffix, "")
    return s
}

private suspend fun fillDOIs(refs: List<String>): List<String> {
    val map = refs.associateWith { it }.toMutableMap()
    val pmids = refs.filter { it.all { ch -> ch.isDigit() } }
    val dois = getDOIsforPMIDs(pmids)
    pmids.forEachIndexed { index, key ->
        val v = dois[index]
        v?.let { map[key] = it }
    }
    return refs.mapNotNull { map[it] }
}