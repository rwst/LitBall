package org.reactome.lit_ball.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.handleException
import java.io.File
import java.util.*
import kotlin.math.min

object PaperList {
    var list: List<Paper> = listOf()
    private var path: String? = null
    var fileName: String = ""
    var query: LitBallQuery? = null
    private var shadowMap: MutableMap<Int, Int> = mutableMapOf()
    private var flagList: List<String>? = null
        get() {
            if (field == null) {
                field = Settings.map["flags"]?.split(" ")
            }
            return field
        }
    fun setFromQuery(query: LitBallQuery, file: File) {
        this.query = query
        fileName = FileType.FILTERED.fileName
        path = file.absolutePath
        readFromFile(file)
        sanitize()
        updateShadowMap()
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
        list = list.toMutableList().apply {
            this[index] = transformer(list[index])
        }.toList()
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
                list = list.toMutableList().apply {
                    this[index] = newPaper
                }.toList()
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

    @OptIn(ExperimentalSerializationApi::class)
    fun readFromFile(file: File) {
        val json = ConfiguredJson.get()
        if (file.isDirectory) throw Exception("Cannot open directory: ${file.name}")

        if (path == null) path = file.absolutePath
        val f = File(file.absolutePath)

        if (f.exists()) {
            val papers = json.decodeFromStream<List<Paper>>(f.inputStream())
            list = papers
        } else {
            throw Exception("File to open: $fileName does not exist")
        }
        runBlocking {
            updateShadowMap()
            delay(200)
            AnnotatingRootStore.refreshList()
            AnnotatingRootStore.refreshClassifierButton()
        }
    }

    fun save() {
        val json = ConfiguredJson.get()
        if (path == null) return
        val pathStr: String = path as String
        val text = json.encodeToString(list)
        File(pathStr).writeText(text)
    }

    fun export() {
        val pathPrefix = path?.substringBeforeLast("/")
        fun writeToPath(tag: Tag, fileType: FileType) {
            val path = "$pathPrefix/${fileType.fileName}"
            list.filter { it.tag == tag }.forEach { item ->
                item.details.externalIds?.get("DOI")?.uppercase()?.let { uppercaseDOI ->
                    File(path).appendText("$uppercaseDOI\n")
                }
            }
        }
        try {
            writeToPath(Tag.Accepted, FileType.ACCEPTED)
            writeToPath(Tag.Rejected, FileType.REJECTED)
        }
        catch (e: Exception) {
            handleException(e)
            return
        }
        path?.let { File(it).delete() }
        println("$path deleted")
        RootStore.setAnnotated()
        AnnotatingRootStore.switchRoot()
        query = null
    }

    fun import(files: List<File>): PaperList {
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

    private fun processLines(lines: List<String>): MutableSet<String> {
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
        val newTag = Tag.entries[btn]
        updateItem(id) {
            it.tag = newTag
            return@updateItem it
        }
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

    fun applyClassifier() {
        AnnotatingRootStore.setClassifierExceptionAlert(true)
    }
}