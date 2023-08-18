@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ConfirmationDialog(
    title: String,
    text: String,
    onCloseClicked: () -> Unit,
    onConfirmClicked: () -> Unit,
) {
    AlertDialog(
        title = { Text(title) },
        onDismissRequest = onCloseClicked,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmClicked()
                    onCloseClicked()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCloseClicked
            ) {
                Text("Cancel")
            }
        },
        text = { Text(text) }
    )
}