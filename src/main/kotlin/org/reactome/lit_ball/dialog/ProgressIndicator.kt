@file:Suppress("FunctionName")
package org.reactome.lit_ball.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProgressIndicator(value: Float, text: String) {
    Box(
        modifier = Modifier
            .progressSemantics(value)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                value,
                strokeWidth = 8.dp,
                modifier = Modifier.size(100.dp).padding(16.dp),
            )
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
            )
        }
    }
}
