package common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import service.S2Interface
import util.UniqueIdGenerator

enum class Tag {
    @SerialName("REJECTED")
    Rejected,

    @SerialName("ACCEPTED")
    Accepted,
}

@Serializable
class Paper(
    var uniqueId: Int = -1,
    val details: S2Interface.PaperDetails = S2Interface.PaperDetails(),
    var tag: Tag = Tag.Rejected,
    var flags: MutableSet<String> = mutableSetOf(),
    var paperId: String? = null,
) {

    fun copy(newTag: Tag = this.tag) = Paper(UniqueIdGenerator.nextId(), details, newTag, flags, paperId)

    override fun toString(): String {
        return "Paper(id=$uniqueId, details=$details, tag=$tag, flags=$flags, paperId=$paperId)"
    }

    fun setPaperIdFromDetails(): Paper {
        paperId = details.externalIds?.get("DOI")?.lowercase()
        if (paperId.isNullOrBlank()) {
            paperId = "s2:${details.paperId}"
        }
        return this
    }

    fun fixNullTldr(): Paper {
        val tldr = details.tldr
        if (tldr != null && tldr["text"] == null) {
            details.tldr = null
        }
        return this
    }

    /**
     * Converts the paper's details into RIS (Research Information Systems) format.
     *
     * The output format is a standard RIS string including fields such as type, ID, DOI, authors,
     * title, journal details, publication date, keywords, abstract, and URLs.
     * Each supported field is translated into its corresponding RIS tag.
     * NOTE: since no ref manager supports multiple URLs (why?) we write the PubMed URL, and if not found,
     * the Semantic Scholar URL.
     *
     * @return A string representation of the paper in RIS format.
     */
    fun toRIS(): String {
        fun formatAuthor(author: String?): String {
            if (author == null || author.isBlank()) return ""
            val words = author.trim().split(" ")
            if (words.size == 1) return words[0]
            return "${words.last()}, ${words.dropLast(1).joinToString(" ")}"
        }
        fun formatPages(pages: String?): Pair<String, String>? {
            if (pages == null) return null
            val cleanPages = pages.replace("\\s".toRegex(), "")
            if (cleanPages.isBlank()) return null
            val parts = cleanPages.split("-", limit = 2)
            return Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
        }
        val sb = StringBuilder()

        // Determine the type of reference (TY)
        val type = when {
            details.publicationTypes?.contains("Conference") == true -> "CONF"
            else -> "JOUR"
        }
        sb.appendLine("TY  - $type")

        // Map paperId to ID
        paperId?.let { sb.appendLine("ID  - $it") }

        // Map DOI from externalIds to DO
        details.externalIds?.get("DOI")?.let {
            sb.appendLine("DO  - $it")
        }

        // Map PMID from externalIds (as "PMID: value")
        details.externalIds?.get("PubMed")?.let {
            sb.appendLine("PMID  - $it")
        }

        // Map PMCID from externalIds (as "PMCID: value")
        details.externalIds?.get("PubMedCentral")?.let {
            sb.appendLine("PMCID  - $it")
        }

        // Map authors to AU (each author on a separate line)
        details.authors?.forEach { sb.appendLine("AU  - ${formatAuthor(it["name"])}") }

        // Map title to TI
        details.title?.let { sb.appendLine("TI  - $it") }

        // Map venue to T2 if type is CONF
        if (type == "CONF") {
            details.venue?.let { sb.appendLine("T2  - $it") }
        }

        // Map journal fields if type is JOUR
        if (type == "JOUR") {
            details.journal?.let { journal ->
                journal["name"]?.let { sb.appendLine("JF  - $it") }
                journal["issn"]?.let { sb.appendLine("SN  - $it") }
                journal["volume"]?.let { sb.appendLine("VL  - $it") }
                journal["pages"]?.let {
                    formatPages(it)?.let { (sp, ep) ->
                        sb.appendLine("SP  - $sp")
                        sb.appendLine("EP  - $ep")
                    }
                }
            }
        }

        // Map abstract to AB
        details.abstract?.let { sb.appendLine("AB  - $it") }

        // Map publicationDate to PY, replacing "-" with "/" for compatibility
        details.publicationDate?.let { date ->
            sb.appendLine("PY  - ${date.subSequence(0, 4)}")
        }
        // Map flags to kW
        flags.forEach { sb.appendLine("KW  - $it") }

        val pmid = details.externalIds?.get("PubMed")
        if (pmid != null && pmid.isNotBlank()) {
            sb.appendLine("UR  - https://pubmed.ncbi.nlm.nih.gov/$pmid/")
        }
        else {
            sb.appendLine("UR  - https://www.semanticscholar.org/paper/${details.paperId}/")
        }

        // End of RIS record
        sb.appendLine("ER  -")

        return sb.toString()
    }
}

fun idSetFromPaperDetailsList(pDList: List<S2Interface.PaperDetails>): MutableSet<String> {
    return idListFromPaperDetailsList(pDList).toMutableSet()
}

fun idListFromPaperDetailsList(pDList: List<S2Interface.PaperDetails>): List<String> {
    return pDList.map { pd ->
        pd.externalIds?.get("DOI")?.let { if (it.isNotBlank()) return@map it.lowercase() }
        "s2:${pd.paperId?.lowercase()}"
    }
}

fun idListFromPaperRefs(refList: List<S2Interface.PaperFullId>): List<String> {
    return refList.map { pd ->
        pd.externalIds?.get("DOI")?.let { if (it.isNotBlank()) return@map it.lowercase() }
        "s2:${pd.paperId?.lowercase()}"
    }
}