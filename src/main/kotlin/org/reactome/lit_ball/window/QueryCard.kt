@file:Suppress("FunctionName")
package org.reactome.lit_ball.window

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.Query

@Composable
fun QueryCard(
    item: Query,
    onClicked: () -> Unit,
) {
    Row(modifier = Modifier.clickable(onClick = onClicked)) {
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = AnnotatedString(item.name),
            modifier = Modifier.weight(1F).align(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(8.dp))

        Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
    }
}
