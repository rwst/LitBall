package org.reactome.lit_ball.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream
import org.reactome.lit_ball.model.Filtering2RootStore
import org.reactome.lit_ball.model.RootStore
import org.reactome.lit_ball.model.Store
import org.reactome.lit_ball.service.NLPService
import org.reactome.lit_ball.service.S2Client
import org.reactome.lit_ball.service.YDFService
import org.reactome.lit_ball.util.ConfiguredJson
import org.reactome.lit_ball.util.Logger
import org.reactome.lit_ball.util.handleException
import java.io.File
import java.io.IOException

object PaperList {
    private const val TAG = "PaperList"
    var list: List<Paper> = listOf()
    private var path: String? = null
    var fileName: String = ""
    var query: LitBallQuery? = null
    var model: Store? = null
    private var shadowMap: MutableMap<Int, Int> = mutableMapOf()
    val flagList: List<String>
        get() {
            return query?.setting?.annotationClasses?.toList() ?: emptyList()
        }

    suspend fun setFromQuery(query: LitBallQuery, file: File, accepted: MutableSet<String>? = null) {
        this.query = query
        fileName = file.name
        path = file.absolutePath
        readFromFile(file, accepted)
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
    suspend fun readFromFile(file: File, accepted: MutableSet<String>?) {
        val json = ConfiguredJson.get()
        if (file.isDirectory) throw Exception("Cannot open directory: ${file.name}")

        if (path == null) path = file.absolutePath
        val f = File(file.absolutePath)
        var papers = if (f.exists()) {
            json.decodeFromStream<List<Paper>>(f.inputStream()).toMutableList()
        } else {
            mutableListOf()
        }
        accepted?.let {
            papers = papers.filter { it.details.externalIds?.get("DOI") in accepted }.toMutableList()
            var maxId = if (papers.isNotEmpty()) papers.maxOf { it.id } else 0
            val acceptedWithDetails = papers.map { it.details.externalIds?.get("DOI")?.uppercase() ?: "" }.toSet()
            val acceptedWithoutDetails = accepted.minus(acceptedWithDetails).toList()
            S2Client.getPaperDetailsWithAbstract(acceptedWithoutDetails) {
                maxId += 1
                val oldDoi = it.externalIds?.get("DOI")
                val doi = oldDoi?.uppercase()
                if (doi != null && doi != oldDoi) {
                    val newExtIds = it.externalIds!!.toMutableMap()
                    newExtIds["DOI"] = doi
                    it.externalIds = newExtIds
                }
                papers.add(Paper(id = maxId, details = it))
            }
        }
        list = papers
        runBlocking {
            updateShadowMap()
            delay(200)
            model?.refreshList()
            model?.refreshClassifierButton()
        }
    }

    fun save() {
        val json = ConfiguredJson.get()
        if (path == null) return
        val pathStr: String = path as String
        val text = json.encodeToString(list)
        File(pathStr).writeText(text)
    }

    fun saveAnnotated() {
        save()
    }

    fun finish() {
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
        } catch (e: Exception) {
            handleException(e)
            return
        }
        path?.let { File(it).delete() }
        println("$path deleted")
        RootStore.setFiltered2()
        Filtering2RootStore.switchRoot()
        query = null
    }

    fun exportAnnotated() {

    }

    fun setTag(id: Int, btn: Int) {
        val newTag = Tag.entries[btn]
        setTag(id, newTag)
    }

    private fun setTag(id: Int, tag: Tag) {
        updateItem(id) {
            if (it.tag == tag)
                it
            else
                Paper(it.id, it.details, tag, it.flags)
        }
    }

    fun setFlag(id: Int, flagNo: Int, value: Boolean) {
        val flag = flagList[flagNo]
        updateItem(id) {
            if (!value)
                flag.let { it1 -> it.flags.add(it1) }
            else
                it.flags.remove(flag)
            it
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

    private const val THRESHOLD = 54
    suspend fun applyClassifier() {
        val classifierName = query?.setting?.classifier ?: ""
        val classifierPath = Settings.map["path-to-classifiers"] + "/" + classifierName
        val modelFile = File(classifierPath)
        if (query == null || classifierName.isBlank() || !modelFile.canRead()) {
            Filtering2RootStore.setClassifierExceptionAlert(true)
            return
        }
        val datasetPath = getQueryDir(query!!.name).absolutePath + "/" + FileType.CLASSIFIER_INPUT.fileName
        val resultPath = getQueryDir(query!!.name).absolutePath + "/" + FileType.CLASSIFIER_OUTPUT.fileName
        writeCsvTo(datasetPath)
        YDFService.path = Settings.map["path-to-YDF"] ?: ""
        val processJob = try {
            YDFService.doPredict(
                modelPath = classifierPath,
                datasetPath = datasetPath,
                resultPath = resultPath,
                key = "doi",
            )
        } catch (e: IOException) {
            Logger.i(TAG, e.toString())
            null
        }
        if (processJob == null) {
            Filtering2RootStore.setYdfNotFoundAlert(true)
            return
        }
        processJob.join()
        val classificationsMap = processCsvFile(resultPath)

        list.forEach { paper ->
            val doi = paper.details.externalIds?.get("DOI")?.uppercase() ?: return@forEach
            val tag = if ((classificationsMap[doi] ?: 0) > THRESHOLD)
                Tag.Accepted
            else
                Tag.Rejected
            setTag(paper.id, tag)
        }
        Filtering2RootStore.refreshList()
    }

    private fun writeCsvTo(path: String) {
        NLPService.init()
        val stringBuilder = StringBuilder()
        stringBuilder.append("text,doi\n")
        list.forEach {
            val text = (it.details.title ?: "") + " " + (it.details.tldr?.get("text") ?: "")
            stringBuilder.append("\"" + NLPService.preprocess(text) + "\",")
            stringBuilder.append("\"${it.details.externalIds?.get("DOI")?.uppercase() ?: ""}\"\n")
        }
        File(path).writeText(stringBuilder.toString())
    }

    private fun processCsvFile(path: String): MutableMap<String, Int> {
        return File(path).readLines().map { it.split(",") }.associateBy(
            { it[2] },
            { (it[1].toFloat() * 100).toInt() }
        ).toMutableMap()
    }
}