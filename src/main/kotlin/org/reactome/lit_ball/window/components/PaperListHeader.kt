package org.reactome.lit_ball.window.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import common.PaperList
import org.reactome.lit_ball.model.PaperListScreenStore

@Composable
fun paperListHeader(
    model: PaperListScreenStore,
    focusRequester: FocusRequester,
    lazyListState: LazyListState,
    alignmentModifier: Modifier
) {
    SortingControls(model.sortingControls)
    FilterControls(model, focusRequester)
    Spacer(modifier = Modifier.width(8.dp))
    TextButton(
        onClick = {},
        modifier = Modifier.padding(0.dp)
    ) {
        Text(PaperList.fileName + " " + lazyListState.firstVisibleItemIndex.toString() + '/' + model.state.items.size.toString())
    }
    Spacer(modifier = Modifier.width(8.dp))
    Tooltip("Save and go back\nto main screen", alignmentModifier) {
        TextButton(
            onClick = { model.onDoAnnotateStopped() },
            modifier = Modifier.padding(0.dp)
        ) {
            Text("Query: ${PaperList.query.name}")
        }
    }
}
