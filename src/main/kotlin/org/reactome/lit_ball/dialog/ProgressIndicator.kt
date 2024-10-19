@file:Suppress("FunctionName")

package org.reactome.lit_ball.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.ExperimentalMaterialApi
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProgressIndicator(parameter: ProgressIndicatorParameter) {
//    var backdropState by remember { mutableStateOf(BackdropValue.Concealed) }

    BackdropScaffold(
        modifier = Modifier.fillMaxSize(),
//        scaffoldState = rememberBackdropScaffoldState(initialValue = backdropState),
        appBar = { /* Optional app bar */ },
        frontLayerBackgroundColor = Color.Transparent,
        frontLayerContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                        progress = { parameter.value },
                        modifier = Modifier.size(100.dp).padding(16.dp),
                        strokeWidth = 8.dp,
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
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        backLayerContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                // No content here, clicking outside the box won't trigger any action
            }
        },
        backLayerBackgroundColor = Color.Black.copy(alpha = 0.5f),
        peekHeight = 0.dp,
    )
}
