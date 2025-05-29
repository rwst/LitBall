@file:Suppress("FunctionName")

package dialog

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

data class DialogParameters(
    val text: String,
    val onConfirmClicked: () -> Unit,
    val onDismissClicked: () -> Unit,
)

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

@Composable
internal fun InformationalDialog(
    title: String,
    dialogParameters: DialogParameters,
) {
    AlertDialog(
        title = { Text(title) },
        onDismissRequest = dialogParameters.onDismissClicked,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = dialogParameters.onDismissClicked
            ) {
                Text("Dismiss")
            }
        },
        text = { Text(dialogParameters.text) }
    )
}