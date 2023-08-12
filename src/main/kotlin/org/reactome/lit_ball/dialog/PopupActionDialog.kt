package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Suppress("FunctionName")
@Composable
internal fun PopupActionDialog(
    labels: List<String>,
    onResult: (buttonClicked: Int) -> Unit,
    onDoneChanged: () -> Unit,
) {
    androidx.compose.material.AlertDialog(
        onDismissRequest = {
            onDoneChanged()
        },
        title = {},
        text = {
            Spacer(modifier = Modifier.height(24.dp))
            Column {
                labels.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onResult(index)
                                onDoneChanged()
                            }
                        ) {
                            Text(text)
                        }
                    }
                }
            }
        },
        buttons = {}
    )
}