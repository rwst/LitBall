package org.reactome.lit_ball.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.*
import kotlin.math.min
import kotlin.reflect.KFunction3

object PaperList {
    private var list: MutableList<Paper> = mutableListOf()
    private var path: String? = null
    var fileName: String = ""
    private var shadowMap: MutableMap<Int, Int> = mutableMapOf()
    var flagList: List<String>? = null
        get() {
            if (field == null) {
                field = Settings.map["flags"]?.split(" ")
            }
            return field
        }

fun toList(): List<Paper> {
        return list.toList()
    }

    fun toListWithItemRemoved(id: Int): List<Paper> {
        list = list.filterNot { it.id == id }.toMutableList()
        updateShadowMap()
        return list.toList()
    }

    private fun updateShadowMap() {
        shadowMap.clear()
        list.forEachIndexed { index, paper ->
            shadowMap[paper.id] = index
        }
    }

    private fun updateItem(id: Int, transformer: (Paper) -> Paper): PaperList {
        val index = shadowMap[id] ?: return this
        list[index] = transformer(list[index])
        return this
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun sanitizeMap(map: Map<String, String>?, onChanged: (MutableMap<String, String>) -> Unit) {
        val extIds = map?.toMutableMap()
        extIds?.entries?.forEach {
            if (it.value == null) {
                extIds.remove(it.key)
                onChanged(extIds)
            }
        }
    }

    private fun sanitize() {
        list.forEachIndexed { index, paper ->
            val newPaper: Paper = paper
            var isChanged = false
            sanitizeMap(paper.details.externalIds) {
                newPaper.details.externalIds = it
                isChanged = true
            }
            sanitizeMap(paper.details.tldr) {
                newPaper.details.tldr = it
                isChanged = true
            }
            if (isChanged)
                list[index] = newPaper
        }
    }

    fun new(files: List<File>): PaperList {
        if (files.size > 1) throw Exception("multiple files selected in New")
        val file = files[0]
        val p: String
        if (file.isDirectory) {
            Settings.map["list-path"] = file.absolutePath
            fileName = "/Untitled"
            p = file.absolutePath + fileName
        } else {
            Settings.map["list-path"] = file.absolutePath.substringBeforeLast('/')
            p = file.absolutePath
            fileName = file.name
        }
        path = p
        Settings.save()
        val f = File(p)
        if (f.exists()) f.delete()
        list = mutableListOf()
        updateShadowMap()
        return this
    }

    fun open(files: List<File>): PaperList {
        Settings.map["list-path"] = files.first().absolutePath.substringBeforeLast('/')
        Settings.save()
        files.forEach {file ->
            readFromFile(file)
        }

        when {
            files.size > 1 -> {
                path = "${path?.substringBeforeLast('/')}/Untitled"
                fileName = "/Untitled"
            }

            files.size == 1 -> {
                path = files.first().absolutePath
                fileName = files.first().name
            }
        }

        list.sortBy { it.details.title }
        list.forEachIndexed { index, paper -> paper.id = index }
        updateShadowMap()
        return this
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readFromFile(file: File) {
        if (file.isDirectory) throw Exception("Cannot open directory: ${file.name}")

        if (path == null) path = file.absolutePath
        val f = File(file.absolutePath)

        if (f.exists()) {
            val papers = Json.decodeFromStream<MutableList<Paper>>(f.inputStream())
            if (list.isEmpty()) {
                list = papers
            } else {
                list.addAll(papers)
            }
        } else {
            throw Exception("File to open: $fileName does not exist")
        }
    }
    fun save() {
        if (path == null) return
        val pathStr: String = path as String
        val text = Json.encodeToString(list)
        File(pathStr).writeText(text)
    }

    val exportFuncs: List<() -> Unit> = listOf(
        { export(processFun = ::chooseEXP) },
    )
    val exportLabels = listOf(
        "Export papers with EXP tag",
        "Export preprocessed",
        "Export flag-specific CSV")
    private fun export(processFun: KFunction3<Paper, String, String, Unit> = ::chooseEXP) {
        path ?: return
        val distinctPapers: MutableMap<String, Paper> = mutableMapOf()

        list.forEach {
            val doi = it.details.externalIds?.get("DOI") ?: return@forEach
            val alreadyFound = distinctPapers[doi]
            if (alreadyFound == null) {
                distinctPapers[doi] = it
            } else {
                if (alreadyFound.tag != it.tag) {
                    println("$doi: ${it.details.title}")
                }
            }
        }
        distinctPapers.forEach { (id, paper) ->
            processFun(paper, id, path as String)
        }
    }
    @Suppress("UNUSED_PARAMETER")
    private fun chooseEXP(paper: Paper, id: String, path: String) {
        if (paper.tag == Tag.Exp) {
            Json.encodeToString(paper).also { json ->
                File("$path-EXP").appendText("$json,\n")
            }
        }
    }

    suspend fun import(files: List<File>): PaperList {
        files.forEach { file ->
            setImportPath(file)
            if (file.isDirectory) return this
            val lines = prepareLines(file)
            val doisRequested = processLines(lines)
            sanitize()
            writeDoisIfNotEmpty(file.absolutePath, doisRequested)
        }
        updateShadowMap()

        return this
    }
    private fun setImportPath(file: File) {
        val importPath =
            if (file.isDirectory)
                file.absolutePath
            else
                file.absolutePath.substringBeforeLast('/')
        Settings.map["import-path"] = importPath
        Settings.save()
    }
    private fun prepareLines(file: File): List<String> {
        return file.readLines()
            .filter { it.isNotBlank() }
            .map {
                it.uppercase(Locale.ENGLISH)
                    .removeSuffix("^M")
                    .trimEnd()
            }
            .toSet()
            .toList()
    }
    private suspend fun processLines(lines: List<String>): MutableSet<String> {
        val doisRequested = lines.toMutableSet()
        val chunkSize = 450
        // TODO: use List.chunk()
        val chunksCount = (lines.size + chunkSize - 1) / chunkSize

        for (n in 1..chunksCount) {
            val maxId: Int = list.maxOfOrNull { it.id } ?: 0
            val upper = min(n * chunkSize - 1, lines.size - 1)
            val lower = (n - 1) * chunkSize

//            S2client.getDataFor(lines.subList(lower, upper + 1))?.mapIndexed { index, paperDetails ->
//                if (paperDetails != null) {
//                    list.add(Paper(maxId + index + 1, paperDetails, Tag.Exp))
//                    doisRequested.remove(paperDetails.externalIds?.get("DOI").toString().uppercase(Locale.ENGLISH))
//                }
//            }
        }

        return doisRequested
    }
    private fun writeDoisIfNotEmpty(path: String, doisRequested: MutableSet<String>) {
        if (doisRequested.isNotEmpty())
            File("$path-DOIs-not-found").writeText(doisRequested.toString())
    }

    fun setTag(id: Int, btn: Int) {
        val newTag = Tag.values()[btn]
        updateItem(id) {
            it.tag = newTag
            return@updateItem it
        }
    }

    fun setAllTags(tag: Tag) {
        list.forEach { it.tag = tag }
    }

    fun setFlag(id: Int, flagNo: Int, value: Boolean) {
        val flag = flagList?.get(flagNo)
        updateItem(id) {
            if (!value)
                flag?.let { it1 -> it.flags.add(it1) }
            else
                it.flags.remove(flag)
            return@updateItem it
        }
    }

    fun stats(): String {
        if (list.isEmpty()) return ""
        var nTLDR = 0
        var nAbstract = 0
        var nTA = 0
        var nPubTypes = 0
        list.forEach {
            var tmp = 0
            it.details.tldr?.get("text")?.let {
                nTLDR += 1
                tmp += 1
            }
            it.details.abstract?.let {
                nAbstract += 1
                tmp += 1
            }
            it.details.publicationTypes.let { nPubTypes += 1 }
            if (tmp == 2) nTA += 1
        }
        return """
            File: $path
            Size: ${list.size}
            #TLDR: ${nTLDR}/${list.size} (${1000 * nTLDR / list.size / 10}%)}
            #Abstracts: ${nAbstract}/${list.size} (${1000 * nAbstract / list.size / 10}%)}
            #both: ${nTA}/${list.size} (${1000 * nTA / list.size / 10}%)}
            #PubType: ${nPubTypes}/${list.size} (${1000 * nPubTypes / list.size / 10}%)}
        """.trimIndent()
    }

    fun pretty(id: Int): String {
        val index = shadowMap[id] ?: return "CAN'T HAPPEN: not in shadowMap"
        val p = list[index]
        return """
            T: ${p.details.title}
            A: ${p.details.abstract}
            TLDR: ${p.details.tldr?.get("text")}
            DOI: ${p.details.externalIds?.get("DOI")}  TYPES: ${p.details.publicationTypes?.joinToString(" ")}
        """.trimIndent()
    }
}