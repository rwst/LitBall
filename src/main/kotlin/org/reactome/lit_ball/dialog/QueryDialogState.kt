package dialog

import androidx.compose.runtime.MutableState
import common.ArticleType
import common.QueryType

interface ArticleTypeState {
    val flagChecked: BooleanArray
    fun withFlagChecked(flagChecked: BooleanArray): ArticleTypeState
}

interface PublicationDateState {
    val pubYear: String
    fun withPubYear(pubYear: String): PublicationDateState
}

interface PaperIdsState {
    val paperIds: String
    val doiWarning: String?
    fun withPaperIds(paperIds: String): PaperIdsState
}

data class ArticleTypeDialogState(
    override val flagChecked: BooleanArray = BooleanArray(ArticleType.entries.size) { true }
) : ArticleTypeState {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArticleTypeDialogState

        return flagChecked.contentEquals(other.flagChecked)
    }

    override fun hashCode(): Int = flagChecked.contentHashCode()

    override fun withFlagChecked(flagChecked: BooleanArray): ArticleTypeState {
        return ArticleTypeDialogState(flagChecked = flagChecked)
    }
}

data class PublicationDateDialogState(
    override val pubYear: String = ""
) : PublicationDateState {
    override fun withPubYear(pubYear: String): PublicationDateState {
        return PublicationDateDialogState(pubYear = pubYear)
    }
}

data class PaperIdsDialogState(
    override val paperIds: String = "",
    override val doiWarning: String? = null
) : PaperIdsState {
    override fun withPaperIds(paperIds: String): PaperIdsState {
        // When user types, update the text and clear any previous warning
        return copy(paperIds = paperIds, doiWarning = null)
    }
}

data class QueryDialogState(
    val copyFrom: String = "",
    val queryType: Int = QueryType.SUPERVISED_SNOWBALLING.ordinal,
    val name: String = "",
    override val paperIds: String = "",
    override val pubYear: String = "",
    override val flagChecked: BooleanArray = BooleanArray(ArticleType.entries.size) { true },
    val check: Boolean = true,
    val nameCheck: Boolean = true,
    val typeWarning: String? = null,
    val pathWarning: String? = null,
    override val doiWarning: String? = null,
) : ArticleTypeState, PublicationDateState, PaperIdsState {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryDialogState

        if (queryType != other.queryType) return false
        if (check != other.check) return false
        if (nameCheck != other.nameCheck) return false
        if (copyFrom != other.copyFrom) return false
        if (paperIds != other.paperIds) return false
        if (name != other.name) return false
        if (pubYear != other.pubYear) return false
        if (!flagChecked.contentEquals(other.flagChecked)) return false
        if (typeWarning != other.typeWarning) return false
        if (pathWarning != other.pathWarning) return false
        if (doiWarning != other.doiWarning) return false

        return true
    }

    override fun hashCode(): Int {
        var result = queryType
        result = 31 * result + check.hashCode()
        result = 31 * result + nameCheck.hashCode()
        result = 31 * result + copyFrom.hashCode()
        result = 31 * result + paperIds.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + pubYear.hashCode()
        result = 31 * result + flagChecked.contentHashCode()
        result = 31 * result + (typeWarning?.hashCode() ?: 0)
        result = 31 * result + (pathWarning?.hashCode() ?: 0)
        result = 31 * result + (doiWarning?.hashCode() ?: 0)
        return result
    }

    override fun withFlagChecked(flagChecked: BooleanArray): ArticleTypeState {
        return this.copy(flagChecked = flagChecked)
    }

    override fun withPubYear(pubYear: String): PublicationDateState {
        return this.copy(pubYear = pubYear)
    }

    override fun withPaperIds(paperIds: String): PaperIdsState {
        return this.copy(
            paperIds = paperIds,
            doiWarning = null // When user types, clear any existing DOI/PMID-related warning
        )
    }
}

inline fun <T> MutableState<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}
