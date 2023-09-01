package org.reactome.lit_ball.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.reactome.lit_ball.common.PaperList

@Suppress("FunctionName")
@Composable
internal fun ItemClickedDialog(id: Int, onDoneClicked: () -> Unit) {
    val scrollState = rememberScrollState()
    Surface (
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(color = Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(500.dp).background(color = Color.White)) {
                Column (modifier = Modifier.fillMaxWidth()) {
                    Row (modifier = Modifier.height(440.dp)) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(scrollState)
                                .weight(weight = .1f, fill = false),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SelectionContainer {
                                Text(
                                    modifier = Modifier
                                        .padding(16.dp),
                                    text = PaperList.pretty(id),
                                    fontSize = 14.sp,
                                )
                            }
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
                            .fillMaxHeight()
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
