package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.reactome.lit_ball.common.PaperList

@Suppress("FunctionName")
@Composable
internal fun ItemClickedDialog(id: Int, onDoneClicked: () -> Unit, focusRequester: FocusRequester) {
    ScrollbarDialog(
        topComposable = {},
        scrollableContent = {
            Text(
                modifier = Modifier
                    .padding(16.dp),
                text = PaperList.pretty(id),
                fontSize = 14.sp,
            )
        },
        onDoneClicked = {
            onDoneClicked()
            focusRequester.requestFocus()
        },
        )
}
