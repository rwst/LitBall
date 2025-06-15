package dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import common.QuerySetting
import common.boolArrayToTypeStrings
import common.typeStringsToBoolArray
import util.StringPatternMatcher
import util.splitToSet
import window.components.*

// Interface for individual setting components
interface QuerySettingEntry {
    @Composable
    fun View()
    fun validate(warnString: MutableState<String?>): Boolean
    fun applyTo(setting: QuerySetting)
}

// --- Implementations of QuerySettingEntry ---

class PaperIdsSettingEntry(initialValue: String) : QuerySettingEntry {
    private val state = mutableStateOf(PaperIdsDialogState(paperIds = initialValue))

    @Composable
    override fun View() {
        queryPaperIdsComponent(state)
    }

    override fun validate(warnString: MutableState<String?>): Boolean = true

    override fun applyTo(setting: QuerySetting) {
    }
}

class MandatoryKeywordsEntry(initialValue: List<String>) : QuerySettingEntry {
    val initialValue = initialValue.joinToString(", ")
    private lateinit var fieldValue: MutableState<String>

    @Composable
    override fun View() {
        fieldValue = rememberSaveable { mutableStateOf(initialValue) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Tooltip(
                text = mandatoryKeywordsTooltipText.trimIndent(),
                Modifier.align(Alignment.CenterVertically)
            ) {
                helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
            }
            Spacer(modifier = Modifier.width(14.dp))
            TextField(
                value = fieldValue.value,
                onValueChange = {
                    fieldValue.value = it
                },
                label = { Text("Mandatory keywords / expression") },
                placeholder = { Text("") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    override fun validate(warnString: MutableState<String?>): Boolean {
        if (!StringPatternMatcher.validateSetting(fieldValue.value)) {
            warnString.value = "Invalid expression"
            return false
        }
        warnString.value = null
        return true
    }

    override fun applyTo(setting: QuerySetting) {
        setting.mandatoryKeyWords.clear()
        setting.mandatoryKeyWords.addAll(StringPatternMatcher.patternSettingFrom(fieldValue.value))
    }
}

class ForbiddenKeywordsEntry(initialValue: List<String>) : QuerySettingEntry {
    val fieldValue = mutableStateOf(initialValue.joinToString(", "))

    @Composable
    override fun View() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tooltip(text = forbiddenKeywordsTooltipText.trimIndent(), Modifier.align(Alignment.CenterVertically)) {
                helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
            }
            Spacer(modifier = Modifier.width(14.dp))
            TextField(
                value = fieldValue.value,
                onValueChange = {
                    fieldValue.value = it
                },
                label = { Text("Forbidden keywords / expression") },
                placeholder = { Text("") },
                modifier = Modifier.weight(1f)
            )
        }
    }
    override fun validate(warnString: MutableState<String?>): Boolean {
        if (!StringPatternMatcher.validateSetting(fieldValue.value)) {
            warnString.value = "Invalid expression"
            return false
        }
        warnString.value = null
        return true
    }

    override fun applyTo(setting: QuerySetting) {
        setting.forbiddenKeyWords.clear()
        setting.forbiddenKeyWords.addAll(StringPatternMatcher.patternSettingFrom(fieldValue.value))
    }
}

class ClassifierEntry(val initialValue: String) : QuerySettingEntry {
    private lateinit var fieldValue: MutableState<String>

    @Composable
    override fun View() {
        fieldValue = rememberSaveable { mutableStateOf(initialValue) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tooltip(text = classifierTooltipText.trimIndent(), Modifier.align(Alignment.CenterVertically)) {
                helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
            }
            Spacer(modifier = Modifier.width(14.dp))
            TextField(
                value = fieldValue.value,
                onValueChange = { fieldValue.value = it },
                label = { Text("Classifier model name") },
                modifier = Modifier.weight(1f)
            )
        }
    }
    override fun validate(warnString: MutableState<String?>): Boolean = true
    override fun applyTo(setting: QuerySetting) {
        setting.classifier = fieldValue.value.trim()
    }
}

class AnnotationClassesEntry(initialValue: List<String>) : QuerySettingEntry {
    private var fieldValue: MutableState<String> = mutableStateOf(initialValue.joinToString(", "))

    @Composable
    override fun View() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tooltip(text = annotationClassesTooltipText.trimIndent(), Modifier.align(Alignment.CenterVertically)) {
                helpIcon(Modifier.size(20.dp).align(Alignment.CenterVertically))
            }
            Spacer(modifier = Modifier.width(14.dp))
            TextField(
                value = fieldValue.value,
                onValueChange = { fieldValue.value = it },
                label = { Text("Annotation classes") },
                placeholder = { Text("text1, text2, ...") },
                modifier = Modifier.weight(1f)
            )
        }
    }
    override fun validate(warnString: MutableState<String?>): Boolean = true
    override fun applyTo(setting: QuerySetting) {
        setting.annotationClasses.clear()
        setting.annotationClasses.addAll(fieldValue.value.splitToSet(","))
    }
}

class PublicationDateSettingEntry(initialPubDate: String) : QuerySettingEntry {
    private val dateState = mutableStateOf(PublicationDateDialogState(pubYear = initialPubDate))

    @Composable
    override fun View() {
        queryPublicationDateComponent(dateState) // Reuses component from QueryDialogComponent.kt
    }

    override fun validate(warnString: MutableState<String?>): Boolean = true // Add specific validation if needed
    override fun applyTo(setting: QuerySetting) {
        setting.pubDate = dateState.value.pubYear.trim()
    }
}

class ArticleTypeSettingEntry(initialPubTypes: List<String>) : QuerySettingEntry {
    private val typeState = mutableStateOf(
        ArticleTypeDialogState(flagChecked = typeStringsToBoolArray(initialPubTypes))
    )

    @Composable
    override fun View() {
        queryArticleTypeComponent(typeState) // Reuses component from QueryDialogComponent.kt
    }
    override fun validate(warnString: MutableState<String?>): Boolean = true
    override fun applyTo(setting: QuerySetting) {
        setting.pubType.clear()
        setting.pubType.addAll(boolArrayToTypeStrings(typeState.value.flagChecked))
    }
}

