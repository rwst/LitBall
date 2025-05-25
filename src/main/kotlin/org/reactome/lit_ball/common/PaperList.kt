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
import util.*
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
        papers.forEachIndexed { index, it ->
            it.setPaperIdFromDetails()
            it.id = index
        }
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
                    newPaper.details.authors = null
                    papers.add(newPaper)
                    list.add(newPaper.details)
                    maxId += 1
                }
                // case not handled: DOIs that are referred to by S2 but don't exist
                if (list.isNotEmpty()) {
                    ArchivedCache.init(getQueryDir(query.name))
                    ArchivedCache.merge(list)
                }
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
    fun saveFiltered() {
        val json = ConfiguredJson.get()
        if (path == null) return
        val pathStr: String = path as String
        val text = json.encodeToString(listHandle.getFullList())
        File(pathStr).writeText(text)
    }

    suspend fun saveAnnotated() {
        ArchivedCache.init(getQueryDir(query.name))
        ArchivedCache.writeArchivedPapers(listHandle.getFullList().toSet())
    }

    /**
     * Writes data to a file path based on the specified tag and file type.
     * Filters a list of objects by the provided tag, extracts the `paperId` values,
     * and writes the combined values with the input set into a file.
     *
     * @param tag The tag used to filter the list of objects.
     * @param fileType The type of file to write to, which determines the file name.
     * @param theSet A mutable set of strings to which filtered `paperId` values are added.
     */
    private suspend fun mergeTaggedToSetAndWriteIds(tag: Tag, fileType: FileType, theSet: MutableSet<String>) {
        val pathPrefix = path?.substringBeforeLast("/") ?: return
        val dir = File(pathPrefix)
        val thisList = listHandle.getFullList().filter { it.tag == tag }
            .mapNotNull { item -> item.paperId }
        theSet += thisList
        writeFile(dir, fileType, theSet.joinToString(separator = "\n", postfix = "\n"))
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
        mergeTaggedToSetAndWriteIds(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
        mergeTaggedToSetAndWriteIds(Tag.Rejected, FileType.REJECTED, query.rejectedSet)
        path?.let { File(it).delete() }
        if (!auto) {
            RootStore.setFiltered2()
            Filtering2RootStore.switchRoot()
            val noAcc = listHandle.getFullList().count { it.tag == Tag.Accepted }
            RootStore.setInformationalDialog("$noAcc papers added to accepted")
            query.let {
                it.noNewAccepted = (noAcc == 0)
                it.writeNoNewAccepted()
            }
            RootStore.refreshList()
        }
    }

    suspend fun delete(id: Int) {
        val p = listHandle.getDisplayedPaper(id) ?: return
        p.paperId?.let {
            query.acceptedSet.remove(it)
            listHandle.delete(it)
            query.rejectedSet.add(it)
        }
        mergeTaggedToSetAndWriteIds(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
        mergeTaggedToSetAndWriteIds(Tag.Rejected, FileType.REJECTED, query.rejectedSet)
        query.syncBuffers()
    }

    suspend fun deleteFiltered() {
        listHandle.getFilteredList()?.let {
            listHandle.deleteAllFiltered()
            val dois = it.map { p -> p.paperId }.toSet()
            query.acceptedSet.removeIf { acc -> dois.contains(acc) }
            mergeTaggedToSetAndWriteIds(Tag.Accepted, FileType.ACCEPTED, query.acceptedSet)
            query.syncBuffers()
        }
    }

    private const val CSV_HEADER = "Title,Review,Date,PMID,PMC,DOI,SScholar,GScholar\n"
    fun exportAnnotated() {
        val pathPrefix = path?.substringBeforeLast("/") ?: return
        val queryDir = File(pathPrefix)

        // Initialize files with headers
        writeFileSync(queryDir, FileType.EXPORTED_CSV, CSV_HEADER)
        writeFileSync(queryDir, FileType.EXPORTED_UNTAGGED_CSV, CSV_HEADER)

        val exportedCatPath = "$pathPrefix/${FileType.EXPORTED_CAT_CSV.fileName}"
        val fileMap = mutableMapOf<String, File>()
        query.setting.annotationClasses.forEach {
            val file = File(exportedCatPath.replace("$", it))
            file.writeText(CSV_HEADER)
            fileMap[it] = file
        }

        val revFile = File(exportedCatPath.replace("$", "Reviews"))
        revFile.writeText(CSV_HEADER)

        // Process each paper
        listHandle.getFullList().forEach {
            val paperId = it.paperId
            val doi = if (paperId?.startsWith("10.") == true) paperId else null
            val date = it.details.publicationDate ?: ""
            val pmid = it.details.externalIds?.get("PubMed")
            val pmc = it.details.externalIds?.get("PubMedCentral")
            val title = it.details.title ?: ""
            val sanTitle = title.replace(",", "%2C")

            // Create output string
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

            // Append to appropriate files
            appendToFileSync(queryDir, FileType.EXPORTED_CSV, outStr)

            if (it.flags.isEmpty()) {
                appendToFileSync(queryDir, FileType.EXPORTED_UNTAGGED_CSV, outStr)
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

    suspend fun exportText() {
        getExtendedDetails()
        val pathPrefix = path?.substringBeforeLast("/") ?: return
        val queryDir = File(pathPrefix)

        // Initialize the file with empty content
        writeFileSync(queryDir, FileType.EXPORTED_JSONL, "")

        val json = ConfiguredUglyJson.get()
        listHandle.getFullList().forEach { thePaper ->
            val id = thePaper.paperId
            id?.let { theId ->
                val meta = mapOf("ID" to JsonPrimitive(theId))
                val outMap = emptyMap<String, JsonElement>().toMutableMap()

                outMap["meta"] = JsonObject(meta)
                val tldr = thePaper.details.tldr?.get("text") ?: ""
                val text = thePaper.details.abstract ?: tldr
                if (text.isNotEmpty()) {
                    outMap["text"] = JsonPrimitive(text)
                    appendToFileSync(queryDir, FileType.EXPORTED_JSONL, json.encodeToString(outMap) + "\n")
                }
            }
        }
    }

    suspend fun exportRIS() {
        getExtendedDetails()
        val out = StringBuilder().apply {
            listHandle.getFullList().forEach { paper ->
                append(paper.toRIS())
            }
        }.toString()

        writeFileSync(getQueryDir(query.name), FileType.EXPORTED_RIS, out)
    }

//    fun exportBibTex() {
//        val pathPrefix = path?.substringBeforeLast("/")
//        val exportedPath = "$pathPrefix/${FileType.EXPORTED_BIBTEX.fileName}"
//        File(exportedPath).writeText("")
//        var out = ""
//        listHandle.getFullList().forEachIndexed { index, thePaper ->
//            var output = "@article{${query.name}-${index},\n"
//            thePaper.details.title.let { output += "title = {${it}},\n" }
//            thePaper.details.abstract?.let { output += "abstract = {${it}},\n" }
//            thePaper.paperId?.let {
//                output += if (it.startsWith("10."))
//                    "doi = {${it}},\n"
//                else
//                    "url = {https://www.semanticscholar.org/paper/${it.substring(3)}},\n"
//            }
//            thePaper.details.externalIds?.let {
//                if (it.containsKey("PubMed"))
//                    output += "url = {https://pubmed.ncbi.nlm.nih.gov/${it["PubMed"]}/},\n"
//            }
//            if (thePaper.flags.isNotEmpty()) {
//                output += "keywords = "
//                thePaper.flags.forEach { flag -> output += "$flag, " }
//                output += "\n"
//            }
//            output += "}\n\n"
//            out += output
//        }
//        File(exportedPath).appendText(out)
//    }

    private suspend fun getExtendedDetails() {
        val paperidsWithoutAuthors = listHandle
            .getFullList()
            .filter { it.details.authors.isNullOrEmpty() }
            .mapNotNull { it.paperId }
        val extendedDetails = mutableListOf<S2Interface.PaperDetails>()
        query.agService.getPaperDetails(
            paperidsWithoutAuthors,
            fields = "authors,journal",
        ) {
            extendedDetails.add(it)
        }
        val map = extendedDetails.associateBy { it.paperId }
        listHandle
            .getFullList()
            .filter { it.details.authors.isNullOrEmpty() }
            .forEach { paper ->
                map[paper.details.paperId]?.let { details ->
                    paper.details.authors = details.authors
                    paper.details.journal = details.journal
                }
            }
        saveAnnotated()
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
        listHandle.setFullTagsFromPaperIdMap(tagMap)
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
