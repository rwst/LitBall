@file:Suppress("FunctionName")

package org.reactome.lit_ball.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.window.Dialog

@Composable
internal fun SettingsDialog(
    setting: QuerySetting,
    onCloseClicked: () -> Unit)
  {
    Dialog(
        title = "Edit settings",
        onCloseRequest = onCloseClicked,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                value = "Path to database storage"
            )

        }
    }
}