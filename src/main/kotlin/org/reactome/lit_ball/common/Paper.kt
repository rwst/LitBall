package common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import service.S2Interface

enum class Tag {
    @SerialName("REJECTED")
    Rejected,

    @SerialName("ACCEPTED")
    Accepted,
}

@Serializable
class Paper(
    var id: Int = -1,
    val details: S2Interface.PaperDetails = S2Interface.PaperDetails(),
    var tag: Tag = Tag.Rejected,
    var flags: MutableSet<String> = mutableSetOf(),
    var paperId: String? = null,
) {

    override fun toString(): String {
        return "Paper(id=$id, details=$details, tag=$tag, flags=$flags, paperId=$paperId)"
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

    fun toRIS(): String {
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
        details.authors?.forEach { sb.appendLine("AU  - $it") }

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
                journal["pageFirst"]?.let { sb.appendLine("SP  - $it") }
                journal["pageLast"]?.let { sb.appendLine("EP  - $it") }
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

        details.externalIds?.get("PubMed")?.let {
            sb.appendLine("UR  - https://pubmed.ncbi.nlm.nih.gov/$it/")
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