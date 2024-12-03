package org.reactome.lit_ball.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Toolkit

@Suppress("FunctionName")
@Composable
internal fun ScrollbarDialog(
    topComposable: @Composable () -> Unit,
    scrollableContent: @Composable () -> Unit,
    onDoneClicked: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val screenHeight = Toolkit.getDefaultToolkit().screenSize.height.dp
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(color = Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(screenHeight * .9f).background(color = Color.White)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    topComposable()
                    Row(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(scrollState)
                                .weight(weight = .1f, fill = false),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            scrollableContent()
                        }
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            adapter = rememberScrollbarAdapter(scrollState)
                        )
                    }
                    ElevatedButton(
                        onClick = onDoneClicked,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(72.dp)
                            .padding(12.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(4.dp),
                    ) {
                        androidx.compose.material3.Text(
                            text = "Dismiss",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
