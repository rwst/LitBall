package dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import common.ArticleType
import common.QueryList
import common.QueryType
import service.getDOIsforPMIDs
import window.components.Icons
import window.components.Tooltip
import window.components.doiInputHelpTooltipText
import window.components.queryTypeTooltipText
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

const val NOCOPY = "---no copy---"

@Suppress("UNCHECKED_CAST")
@Composable
fun <T : ArticleTypeState> queryArticleTypeComponent(
    state: MutableState<T>,
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
                                    state.set { state.value.withFlagChecked(flagChecked = newFlagChecked) as T }
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 0.dp)
                        )
                        Text(
                            text = articleType.s2name,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T: PublicationDateState> queryPublicationDateComponent(
    state: MutableState<T>,
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
                state.set { state.value.withPubYear(pubYear = it) as T }
            },
            label = { Text("Publication Date (optional)") },
            placeholder = { Text("1900-") }
        )
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T: PaperIdsState> queryPaperIdsComponent(
    state: MutableState<T>,
) {
    Row {
        Tooltip(
            text = doiInputHelpTooltipText.trimIndent(),
            Modifier.align(Alignment.CenterVertically)
        ) {
            helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth().height(200.dp)
        ) {
            val scrollState = rememberScrollState()
            TextField(
                value = state.value.paperIds,
                onValueChange = { str ->
                    val newPaperIds = str
                        .split('\n')
                        .joinToString("\n") { s -> s.trim().transformDOI() }
                    state.value = state.value.withPaperIds(paperIds = newPaperIds) as T
                },
                label = { Text("Core DOIs/PMIDs (one per line)") },
                placeholder = { Text("10.XYZ/ABC\n12345678") },
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
            )
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                // 5. Link the scrollbar to the same scroll state.
                adapter = rememberScrollbarAdapter(
                    scrollState = scrollState
                )
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
                state.set { state.value.copy(queryType = btn) }           }
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
                name, paperIds and specific query settings are copied.
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
                    (listOf(NOCOPY) + QueryList.list.value.map { it.name }).forEach { name ->
                        DropdownMenuItem(
                            onClick = {
                                copyFromIsSetValue.value = false
                                state.set { copy(copyFrom = if (name != NOCOPY) name else "") }
                            })
                        { Text(name) }
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