@file:Suppress("FunctionName")

package org.reactome.lit_ball.window.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.reactome.lit_ball.common.LitBallQuery
import org.reactome.lit_ball.common.QueryStatus
import org.reactome.lit_ball.util.formatDateToyyyyMMMddFormat
import org.reactome.lit_ball.window.MARGIN_SCROLLBAR

@Composable
fun QueryCard(
    item: LitBallQuery,
    onClicked: () -> Unit,
    onSettingsClicked: (Int?) -> Unit,
    onGoClicked: (status: QueryStatus, id: Int) -> Unit,
    onAnnotateClicked: () -> Unit,
) {
    ElevatedCard {
        Row(modifier = Modifier.clickable(onClick = onClicked)) {
            IconButton(
                onClick = { (onSettingsClicked)(item.id) },
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
            Column {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = AnnotatedString(item.name),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.Start),
                )
                Text(
                    text = "Accepted: ${item.nrAccepted()}",
                    fontSize = 14.sp,
                    fontWeight = if (item.noNewAccepted) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = "Rejected: ${item.nrRejected()}\nStatus: ${item.status}\nLast expansion: ${
                    formatDateToyyyyMMMddFormat(
                        item.lastExpansionDate
                    )
                }",
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(vertical = 8.dp),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            ElevatedButton(
                onClick = { (onGoClicked)(item.status, item.id) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(6.dp),
            ) {
                Text(item.nextActionText())
            }
            ElevatedButton(
                onClick = onAnnotateClicked,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                elevation = ButtonDefaults.elevatedButtonElevation(4.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                Text(
                    text = "Annotate\naccepted",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
        }
    }
}
