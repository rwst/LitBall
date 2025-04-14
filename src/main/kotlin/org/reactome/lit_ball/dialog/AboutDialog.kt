package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.About
import common.Changes

@Suppress("FunctionName")
@Composable
internal fun AboutDialog(onDoneClicked: () -> Unit) {
    ScrollbarDialog(
        topComposable = {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = About.text,
                fontSize = 14.sp,
            )
        },
        scrollableContent = {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = Changes.text,
                fontSize = 14.sp,
            )
        },
        onDoneClicked = onDoneClicked,
    )
}
