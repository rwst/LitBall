package window.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import model.PaperListScreenStore

@Suppress("FunctionName")
@Composable
internal fun FilterControls(model: PaperListScreenStore, focusRequester: FocusRequester) {
    val filterToggle = model.state.filterDialog
    Row {
        Tooltip(
            if (!filterToggle) "Activate filter" else "Remove filter",
            Modifier.align(Alignment.CenterVertically)
        ) {
            IconButton(
                onClick = {
                    model.setFilterDialog(!filterToggle)
                    focusRequester.requestFocus()
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            ) {
                Icon(
                    painterResource(if (!filterToggle) Icons.FilterList else Icons.FilterListOff),
                    contentDescription = null,
                    tint = Color.Blue,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}