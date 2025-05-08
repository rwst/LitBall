package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import common.ArticleType
import common.QueryList
import common.QueryType
import common.Settings
import service.getDOIsforPMIDs
import window.components.Icons
import window.components.Tooltip
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val queryTypeTooltipText = """
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
                        4. Similarity search: give some DOI/PMID(s) and get a number
                           of "recommended papers" from S2
                    """

private const val doiInputHelpTooltipText = """
                            Input one DOI/PMID per line. It is not necessary to manually trim
                            the DOI strings. LitBall will automatically chop off everything
                            before the “10.” part, so simply copypasting a DOI link will be
                            handled. PMID links will be stripped to just the number"""

@Composable
fun queryArticleTypeComponent(
    state: MutableState<QueryDialogState>,
) {
    Row {
        Tooltip(
            text = """
                            (Optional) Check one or more article types""".trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ArticleType.entries.forEach { articleType ->
                    val (checkedState, onStateChange) = remember { mutableStateOf(state.value.flagChecked[articleType.ordinal]) }
                    Row(
                        modifier = Modifier
                            .padding(vertical = 0.dp)
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedState,
                            onCheckedChange = {
                                if (!checkedState || state.value.flagChecked.count { it } > 1) {
                                    onStateChange(!checkedState)
                                    val newFlagChecked = state.value.flagChecked.clone()
                                    newFlagChecked[articleType.ordinal] = !checkedState
                                    state.set { copy(flagChecked = newFlagChecked) }
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 0.dp)
                        )
                        Text(
                            text = articleType.s2name,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun queryPublicationDateComponent(
    state: MutableState<QueryDialogState>,
) {
    Row {
        Tooltip(
            text = """
                            (Optional) Input a range of years to filter publication dates of articles""".trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        TextField(
            value = state.value.pubYear,
            onValueChange = {
                state.set { copy(pubYear = it) }
            },
            label = { Text("Publication Date (optional)") },
            placeholder = { Text("1900-") }
        )
    }
}

@Composable
fun queryPaperIdsComponent(
    state: MutableState<QueryDialogState>,
) {
    Row {
        Tooltip(
            text = doiInputHelpTooltipText.trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        TextField(
            value = state.value.field,
            onValueChange = { str ->
                state.set {
                    copy(
                        field = str
                            .split('\n')
                            .joinToString("\n") { s -> s.trim().transformDOI() } ,
                        pathWarning = null
                    )
                }
            },
            label = { Text("Core DOIs/PMIDs (one per line)") },
            placeholder = { Text("10.XYZ/ABC\n12345678") }
        )
        state.value.doiWarning?.also {
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

@Composable
fun queryNameComponent(
    state: MutableState<QueryDialogState>,
) {
    Row {
        Tooltip(
            text = """
                        Enter name of query. The string is cut off
                        at the first '/' (slash) character.
                    """.trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        TextField(
            value = state.value.name,
            onValueChange = {
                state.set { copy(name = it.substringBefore("/")) }
            },
            label = { Text("Query name") },
        )
        state.value.pathWarning?.also {
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

@Composable
fun queryTypeComponent(
    state: MutableState<QueryDialogState>,
) {
    Row {
        Tooltip(
            text = queryTypeTooltipText.trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        RadioButtonOptions(
            QueryType.entries.map { it.pretty },
            state.value.queryType,
            onOptionSelected = { btn ->
                state.set {
                    if ((btn == 0 || btn == 3) && Settings.map["S2-API-key"].isNullOrEmpty()) {
                        copy(typeWarning = "S2 API key needed")
                    } else {
                        copy(typeWarning = null)
                    }
                }
                state.set { copy(queryType = btn) }
            }
        )
        state.value.typeWarning?.also {
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

@Composable
fun queryCopyFromComponent(
    state: MutableState<QueryDialogState>,
) {
    val copyFromIsSetValue = rememberSaveable { mutableStateOf(false) }

    Row {
        Tooltip(
            text = """
                If set, allows selection of existing query, from which
                name, DOIs and specific query settings are copied.
                    """.trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        TextButton(
            onClick = { copyFromIsSetValue.value = !copyFromIsSetValue.value },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) { Text("Copy from query: ${state.value.copyFrom}") }
        Spacer(modifier = Modifier.width(14.dp))
        if (copyFromIsSetValue.value) {
            Box {
                DropdownMenu(
                    expanded = copyFromIsSetValue.value,
                    onDismissRequest = {
                        state.set { copy(copyFrom = "") }
                        copyFromIsSetValue.value = false })
                {
                    QueryList.list.map { it.name }.forEach {
                        DropdownMenuItem(
                            onClick = {
                                state.set { copy(copyFrom = it) }
                                copyFromIsSetValue.value = false
                            })
                        { Text(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun helpIcon(modifier: Modifier) {
    Icon(
        painterResource(Icons.Help),
        contentDescription = "Query Settings",
        tint = Color.Gray,
        modifier = modifier,
    )
}

val pmidRegex = "^pmid:*\\h*".toRegex(RegexOption.IGNORE_CASE)
val slashSuffix = "/$".toRegex()
fun String.transformDOI(): String {
    var s = this.lowercase()
    if (s.startsWith("HTTP"))
        s = URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
    s = s.replaceBefore("10.", "")
    s = s.replace(pmidRegex, "").replace("HTTPS://PUBMED.NCBI.NLM.NIH.GOV/", "")
    s = s.replace(slashSuffix, "")
    return s
}

suspend fun fillDOIs(refs: List<String>): List<String> {
    val map = refs.associateWith { it }.toMutableMap()
    val pmids = refs.filter { it.all { ch -> ch.isDigit() } }
    val dois = getDOIsforPMIDs(pmids)
    pmids.forEachIndexed { index, key ->
        val v = dois[index]
        v?.let { map[key] = it }
    }
    return refs.mapNotNull { map[it] }
}