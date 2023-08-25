@file:Suppress("FunctionName")
package org.reactome.lit_ball.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProgressIndicatorParameter(
    val title: String,
    val value: Float,
    val text: String,
    val onCancelClicked: () -> Unit,
)
@Composable
fun ProgressIndicator(parameter: ProgressIndicatorParameter) {
    Box(
        modifier = Modifier
            .progressSemantics(parameter.value)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = parameter.title,
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp)
                )
            CircularProgressIndicator(
                parameter.value,
                strokeWidth = 8.dp,
                modifier = Modifier.size(100.dp).padding(16.dp),
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                parameter.text,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
            )
            ElevatedButton(
                onClick = parameter.onCancelClicked,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                elevation = ButtonDefaults.elevatedButtonElevation(4.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                androidx.compose.material.Text(
                    text ="Cancel",
                    fontSize = 14.sp
                )
            }
        }
    }
}
