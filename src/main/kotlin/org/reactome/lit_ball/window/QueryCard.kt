@file:Suppress("FunctionName")
package org.reactome.lit_ball.window

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryCard(
    item: Query,
    onClicked: () -> Unit,
) {
    ElevatedCard {
        Row(modifier = Modifier.clickable(onClick = onClicked)) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Query Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            SuggestionChip (
                onClick = {},
                label = {
                    Text(
                        text = AnnotatedString(item.name),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    ) },
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = "accepted: 844\nrejected: 7915\nstatus: ${item.status}",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.width(4.dp))

            FilledTonalButton (
                onClick = item.nextAction(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            ) {
                Text(item.nextActionText())
            }

            Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
        }
    }
}
