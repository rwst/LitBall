@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun RadioButtonOptions(
    options: List<String>,
    defaultSelectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    var selectedOptionIndex by remember { mutableStateOf(defaultSelectedOptionIndex) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .toggleable(
                        value = index == selectedOptionIndex,
                        role = Role.RadioButton,
                        onValueChange = {
                            selectedOptionIndex = index
                            onOptionSelected(selectedOptionIndex)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = index == selectedOptionIndex,
                    onClick = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = option,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun FlagBoxes(
    flags: List<String>,
    checkedFlags: Set<String>,
    onFlagChanged: (Int, Boolean) -> Unit,
) {
    Column {
        val nColsPerRow = 8
        val nRows = (flags.size + nColsPerRow - 1) / nColsPerRow
        val nCols = if (flags.size <= nColsPerRow) flags.size else nColsPerRow
        val rowHeights = listOf(20.dp, 20.dp, 20.dp, 16.dp, 12.dp, 12.dp, 12.dp, 12.dp, 12.dp)
        val textHeights = listOf(18.sp, 16.sp, 12.sp, 10.sp, 8.sp, 8.sp, 8.sp, 8.sp, 8.sp)
        val boxScales = listOf(1.0f, 1.0f, 0.8f, 0.7f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
        for (rowNr in 1..nRows) {
            Row(
                modifier = Modifier
                    .height(rowHeights[nRows]),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (colNr in 1..nCols) {
                    val flagNr = nColsPerRow * (rowNr - 1) + colNr
                    if (flagNr > flags.size) continue
                    val (checkedState, onStateChange) = remember { mutableStateOf(flags[flagNr - 1] in checkedFlags) }
                    Column(
                        modifier = Modifier
                            .absolutePadding(left = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .absolutePadding(left = 0.dp)
                                .toggleable(
                                    value = true,
                                    onValueChange = {
                                        onStateChange(!checkedState)
                                        onFlagChanged(flagNr - 1, checkedState)
                                    },
                                    role = Role.Checkbox
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedState,
                                onCheckedChange = {
                                    onStateChange(!checkedState)
                                    onFlagChanged(flagNr - 1, !it)
                                },
                                modifier = Modifier
                                    .scale(boxScales[nRows])
                                    .padding(horizontal = 0.dp, vertical = 0.dp),
                            )
                            Text(
                                text = flags[flagNr - 1],
                                fontSize = textHeights[nRows],
                                style = TextStyle(lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None)),
                                modifier = Modifier
                                    .padding(top = 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
