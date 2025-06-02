package dialog

import androidx.compose.runtime.MutableState
import common.ArticleType
import common.QueryType

interface ArticleTypeState {
    val flagChecked: BooleanArray
    fun update(flagChecked: BooleanArray): ArticleTypeState
}

interface PublicationDateState {
    val pubYear: String
    fun update(pubYear: String): PublicationDateState
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

    override fun update(flagChecked: BooleanArray): ArticleTypeState {
        return ArticleTypeDialogState(flagChecked = flagChecked)
    }
}

data class PublicationDateDialogState(
    override val pubYear: String = ""
) : PublicationDateState {
    override fun update(pubYear: String): PublicationDateState {
        return PublicationDateDialogState(pubYear = pubYear)
    }
}

data class QueryDialogState(
    val copyFrom: String = "",
    val queryType: Int = QueryType.SUPERVISED_SNOWBALLING.ordinal,
    val field: String = "",
    val name: String = "",
    override val pubYear: String = "",
    override val flagChecked: BooleanArray = BooleanArray(ArticleType.entries.size) { true },
    val check: Boolean = true,
    val nameCheck: Boolean = true,
    val typeWarning: String? = null,
    val pathWarning: String? = null,
    val doiWarning: String? = null
) : ArticleTypeState, PublicationDateState {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryDialogState

        if (queryType != other.queryType) return false
        if (check != other.check) return false
        if (nameCheck != other.nameCheck) return false
        if (copyFrom != other.copyFrom) return false
        if (field != other.field) return false
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
        result = 31 * result + field.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + pubYear.hashCode()
        result = 31 * result + flagChecked.contentHashCode()
        result = 31 * result + (typeWarning?.hashCode() ?: 0)
        result = 31 * result + (pathWarning?.hashCode() ?: 0)
        result = 31 * result + (doiWarning?.hashCode() ?: 0)
        return result
    }

    override fun update(flagChecked: BooleanArray): ArticleTypeState {
        return QueryDialogState(
            copyFrom = copyFrom,
            field = field,
            name = name,
            pubYear = pubYear,
            flagChecked = flagChecked,
            queryType = queryType,
            typeWarning = typeWarning,
            pathWarning = pathWarning,
            doiWarning = doiWarning,
        )
    }

    override fun update(pubYear: String): PublicationDateState {
        return QueryDialogState(
            copyFrom = copyFrom,
            field = field,
            name = name,
            pubYear = pubYear,
            flagChecked = flagChecked,
            queryType = queryType,
            typeWarning = typeWarning,
            pathWarning = pathWarning,
            doiWarning = doiWarning,
        )
    }
}

inline fun <T> MutableState<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}
