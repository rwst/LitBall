package common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import model.Filtering2RootStore
import model.PaperListScreenStore
import model.RootStore
import service.NLPService
import service.S2Interface
import service.YDFService
import util.ConfiguredJson
import util.ConfiguredUglyJson
import util.Logger
import util.handleException
import window.components.SortingType
import java.io.File
import java.io.IOException

object PaperList {
    private const val TAG = "PaperList"
    var listHandle = PaperListHandle()
    private var path: String? = null
    var fileName: String = ""
    lateinit var query: LitBallQuery
    var model: PaperListScreenStore? = null
    val flagList: List<String>
        get() {
            return query.setting.annotationClasses.toList()
        }

    suspend fun setFromQuery(query: LitBallQuery, file: File, accepted: MutableSet<String>? = null) {
        PaperList.query = query
        fileName = file.name
        path = file.absolutePath
        readAcceptedDetailsFromFile(file, accepted)
    }

    fun toList(): List<Paper> {
        return listHandle.getList()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readAcceptedDetailsFromFile(file: File, accepted: MutableSet<String>?) {
        val json = ConfiguredJson.get()
        var papers = withContext(Dispatchers.IO) {
            if (file.isDirectory) throw Exception("Cannot open directory: ${file.name}")
            if (path == null) path = file.absolutePath
            if (file.exists()) {
                json.decodeFromStream<List<Paper>>(file.inputStream()).toMutableList()
            } else {
                mutableListOf()
            }
        }
        papers.forEach { it.setPaperIdFromDetails() }
        accepted?.let {
            papers = papers.distinctBy { it.paperId }.filter { it.paperId in accepted }.toMutableList()
            var maxId = papers.size
            val acceptedWithDetails = papers.map { it.paperId ?: "" }.toSet()
            val acceptedWithoutDetails = accepted.minus(acceptedWithDetails).toList()
            if (acceptedWithoutDetails.isNotEmpty()) {
                val list: MutableList<S2Interface.PaperDetails> = mutableListOf()
                query.agService.getPaperDetails(
                    acceptedWithoutDetails,
                    fields = "paperId,externalIds,title,abstract,publicationTypes,tldr,publicationDate",
                ) {
                    val newPaper = Paper(id = maxId, details = it)
                    newPaper.setPaperIdFromDetails().fixNullTldr()
                    papers.add(newPaper)
                    list.add(newPaper.details)
                    maxId += 1
                }
                // case not handled: DOIs that are referred to by S2 but don't exist
                if (list.isNotEmpty()) { query.mergeIntoArchive(list) }
            }
        }
        listHandle.setFullList(papers)
        runBlocking {
            sort(
                SortingType.valueOf(
                    Settings.map["paper-sort-type"] ?: SortingType.ALPHA_ASCENDING.toString()
                )
            )
            delay(200) // TODO
            model?.refreshList()
            model?.refreshClassifierButton()
        }
    }

    /**
     * Save current paper details to current path/file, which is FILTERED1 or ARCHIVED.
     * Does NOT advance query status.
     */
    fun save() {
        val json = ConfiguredJson.get()
        if (path == null) return
        val pathStr: String = path as String
        val text = json.encodeToString(listHandle.getFullList())
        File(pathStr).writeText(text)
    }

    fun saveAnnotated() {
        save()
    }

    private fun writeToPath(tag: Tag, fileType: FileType, theSet: MutableSet<String>) {
        val pathPrefix = path?.substringBeforeLast("/") ?: return
        val pathStr = "$pathPrefix/${fileType.fileName}"
        val thisList = listHandle.getFullList().filter { it.tag == tag }
            .mapNotNull { item -> item.paperId }
        theSet += thisList
        File(pathStr).writeText(theSet.joinToString(separator = "\n", postfix = "\n"))
    }

    fun acceptFiltered(value: Boolean) {
        listHandle.setFilteredAllTags(if (value) Tag.Accepted else Tag.Rejected)
        listHandle.setFullTagsFromFiltered()
    }

    /**
     * Finish filtering2 phase by saving accepted/rejected DOIs to their files, adding them to the sets in memory,
     * deleting the FILTERED1 file.
     *
     * @param auto When auto is not set, a note appears with the number of accepted papers.
     */
    suspend fun finish(auto: Boolean = false) {
        try {
            writeToPath(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
            writeToPath(Tag.Rejected, FileType.REJECTED, query.rejectedSet)
        } catch (e: Exception) {
            handleException(e)
            return
        }
        path?.let { File(it).delete() }
        if (!auto) {
            RootStore.setFiltered2()
            Filtering2RootStore.state.paperListStore.switchRoot()
            val noAcc = listHandle.getFullList().count { it.tag == Tag.Accepted }
            RootStore.setInformationalDialog("$noAcc papers added to accepted")
            query.let {
                it.noNewAccepted = (noAcc == 0)
                it.writeNoNewAccepted()
            }
            RootStore.refreshList()
        }
    }

    fun delete(id: Int) {
        val p = listHandle.getDisplayedPaper(id) ?: return
        query.acceptedSet.removeIf { acc -> p.paperId?.let { it == acc } ?: false }
        listHandle.delete(p.paperId)
        try {
            writeToPath(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
        } catch (e: Exception) {
            handleException(e)
        }
        query.syncBuffers()
    }

    fun deleteFiltered() {
        val fList = listHandle.getFilteredList()
        fList?.let {
            listHandle.deleteAllFiltered()
            val dois = it.map { p -> p.paperId }.toSet()
            query.acceptedSet.removeIf { acc -> dois.contains(acc) }
            try {
                writeToPath(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
            } catch (e: Exception) {
                handleException(e)
            }
            query.syncBuffers()
        }
    }

    private const val CSV_HEADER = "Title,Review,Date,PMID,PMC,DOI,SScholar,GScholar\n"
    fun exportAnnotated() {
        val pathPrefix = path?.substringBeforeLast("/")
        val exportedPath = "$pathPrefix/${FileType.EXPORTED_CSV.fileName}"
        File(exportedPath).writeText(CSV_HEADER)
        val exportedUntaggedPath = "$pathPrefix/${FileType.EXPORTED_UNTAGGED_CSV.fileName}"
        File(exportedUntaggedPath).writeText(CSV_HEADER)
        val exportedCatPath = "$pathPrefix/${FileType.EXPORTED_CAT_CSV.fileName}"
        val fileMap = mutableMapOf<String, File>()
        query.setting.annotationClasses.forEach {
            val file = File(exportedCatPath.replace("$", it))
            file.writeText(CSV_HEADER)
            fileMap[it] = file
        }
        val revFile = File(exportedCatPath.replace("$", "Reviews"))
        revFile.writeText(CSV_HEADER)
        listHandle.getFullList().forEach {
            val paperId = it.paperId
            val doi = if (paperId?.startsWith("10.") == true) paperId else null
            val date = it.details.publicationDate ?: ""
            val pmid = it.details.externalIds?.get("PubMed")
            val pmc = it.details.externalIds?.get("PubMedCentral")
            val title = it.details.title ?: ""
            val sanTitle = title.replace(",", "%2C")
            val outStr = java.lang.StringBuilder()
                .append("\"$title\",")
                .append(if (it.details.publicationTypes?.contains("Review") == true) "✔," else ",")
                .append("$date,")
                .append(if (pmid != null) "https://pubmed.ncbi.nlm.nih.gov/$pmid/," else ",")
                .append(if (pmc != null) "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC$pmc/," else ",")
                .append(if (doi != null) "https://doi.org/$doi," else ",")
                .append("https://www.semanticscholar.org/paper/${it.details.paperId},")
                .append(
                    if (title.isEmpty()) "" else
                        "https://scholar.google.de/scholar?hl=en&as_sdt=0%2C5&q=${sanTitle.replace(" ", "+")}&btnG="
                )
                .append("\n")
                .toString()
            File(exportedPath).appendText(outStr)
            if (it.flags.isEmpty()) {
                File(exportedUntaggedPath).appendText(outStr)
            }
            else {
                it.flags.forEach { flag ->
                    fileMap[flag]?.appendText(outStr)
                }
            }
            if (it.details.publicationTypes?.contains("Review") == true)
                revFile.appendText(outStr)
        }
    }

    fun exportText() {
        val pathPrefix = path?.substringBeforeLast("/")
        val exportedPath = "$pathPrefix/${FileType.EXPORTED_JSONL.fileName}"
        File(exportedPath).writeText("")
        val json = ConfiguredUglyJson.get()
        listHandle.getFullList().forEach { thePaper ->
            val doi = thePaper.paperId
            doi?.let { theDoi ->
                val meta = mapOf("DOI" to JsonPrimitive(theDoi))
                val outMap = emptyMap<String, JsonElement>().toMutableMap()

                outMap["meta"] = JsonObject(meta)
                val tldr = thePaper.details.tldr?.get("text") ?: ""
                val text = thePaper.details.abstract ?: tldr
                if (text.isNotEmpty()) {
                    outMap["text"] = JsonPrimitive(text)
                    File(exportedPath).appendText(json.encodeToString(outMap) + "\n")
                }
            }
        }
    }

    fun exportBibTex() {
        val pathPrefix = path?.substringBeforeLast("/")
        val exportedPath = "$pathPrefix/${FileType.EXPORTED_BIBTEX.fileName}"
        File(exportedPath).writeText("")
        var out = ""
        listHandle.getFullList().forEachIndexed { index, thePaper ->
            var output = "@article{${query.name}-${index},\n"
            thePaper.details.title.let { output += "title = {${it}},\n" }
            thePaper.details.abstract?.let { output += "abstract = {${it}},\n" }
            thePaper.paperId?.let {
                output += if (it.startsWith("10."))
                    "doi = {${it}},\n"
                else
                    "url = {https://www.semanticscholar.org/paper/${it.substring(3)}},\n"
            }
            thePaper.details.externalIds?.let {
                if (it.containsKey("PubMed"))
                    output += "url = {https://pubmed.ncbi.nlm.nih.gov/${it["PubMed"]}/},\n"
            }
            if (thePaper.flags.isNotEmpty()) {
                output += "keywords = "
                thePaper.flags.forEach { flag -> output += "$flag, " }
                output += "\n"
            }
            output += "}\n\n"
            out += output
        }
        File(exportedPath).appendText(out)
    }

    fun setTag(id: Int, btn: Int) {
        val newTag = Tag.entries[btn]
        listHandle.setTag(id, newTag)
    }

    fun sort(type: SortingType) {
        listHandle.sort(type)
        Settings.map["paper-sort-type"] = type.toString()
        Settings.save()
    }

    fun pretty(id: Int): String {
        val p = listHandle.getDisplayedPaper(id) ?: return "CAN'T HAPPEN: shadowMap[id] == null"
        val pmId = p.details.externalIds?.get("PubMed")
        val textPMID = if (pmId != null) "PMID: $pmId" else ""
        return """
            T: ${p.details.title}
            DATE: ${p.details.publicationDate} $textPMID
            A: ${p.details.abstract}
            TLDR: ${p.details.tldr?.get("text")}
            DOI: ${p.paperId}  TYPES: ${p.details.publicationTypes?.joinToString(" ")}
        """.trimIndent()
    }

    private const val THRESHOLD = 54
    suspend fun applyClassifier() {
        val classifierName = query.setting.classifier
        val classifierPath = Settings.map["path-to-classifiers"] + "/" + classifierName
        val modelFile = File(classifierPath)
        if (classifierName.isBlank() || !modelFile.canRead()) {
            model?.setClassifierExceptionAlert(true)
            return
        }
        val datasetPath = getQueryDir(query.name).absolutePath + "/" + FileType.CLASSIFIER_INPUT.fileName
        val resultPath = getQueryDir(query.name).absolutePath + "/" + FileType.CLASSIFIER_OUTPUT.fileName
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
            model?.setYdfNotFoundAlert(true)
            return
        }
        processJob.join()
        val classificationsMap = processCsvFile(resultPath)
        val tagMap: Map<String, Tag> = classificationsMap.mapValues { (_, value) ->
            if (value > THRESHOLD)
                Tag.Accepted
            else
                Tag.Rejected
        }
        listHandle.setFullTagsFromDoiMap(tagMap)
        Filtering2RootStore.state.paperListStore.refreshList()
    }

    fun applyFilter(filterString: String) {
        listHandle.applyFilter(filterString)
    }

    private fun writeCsvTo(path: String) {
        NLPService.init()
        val stringBuilder = StringBuilder()
        stringBuilder.append("text,doi\n")
        listHandle.getFullList().forEach {
            val text = (it.details.title ?: "") + " " + (it.details.tldr?.get("text") ?: "")
            stringBuilder.append("\"" + NLPService.preprocess(text) + "\",")
            stringBuilder.append("\"${it.paperId ?: ""}\"\n")
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