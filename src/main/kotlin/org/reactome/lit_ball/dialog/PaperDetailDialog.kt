package dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.PaperList

@Suppress("FunctionName")
@Composable
internal fun PaperDetailDialog(id: Long, onDoneClicked: () -> Unit, focusRequester: FocusRequester) {
    ScrollbarDialog(
        topComposable = {},
        scrollableContent = {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    text = PaperList.pretty(id),
                    fontSize = 14.sp,
                )
            }
        },
        onDoneClicked = {
            onDoneClicked()
            focusRequester.requestFocus()
        },
    )
}
