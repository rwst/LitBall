@file:Suppress("FunctionName")

package org.reactome.lit_ball.window.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.LitBallQuery
import common.QueryStatus
import common.QueryType
import org.reactome.lit_ball.util.formatDateToyyyyMMMddFormat
import org.reactome.lit_ball.window.MARGIN_SCROLLBAR

@Composable
fun QueryCard(
    item: LitBallQuery,
    onClicked: () -> Unit,
    onSettingsClicked: (Int?) -> Unit,
    onGoClicked: (status: QueryStatus, id: Int) -> Unit,
    onAnnotateClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    val chipColors = listOf(
        Color.hsv(186F, 1F, 1F),
        Color.hsv(163f, 1f, 1f),
        Color.hsv(0f, 0f, .9f),
        Color.hsv(150f, 1f, .9f),
    )
    ElevatedCard {
        Row(modifier = Modifier.clickable(onClick = onClicked)) {
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Tooltip("Query-specific settings", Modifier.align(Alignment.CenterHorizontally)) {
                    IconButton(
                        onClick = { (onSettingsClicked)(item.id) },
                        modifier = Modifier
                            .size(height = 30.dp, width = 30.dp)
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            painter = painterResource(Icons.Settings),
                            contentDescription = "Query Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                }
                Spacer(modifier = Modifier.fillMaxHeight())
                Tooltip("Delete query on disk", Modifier.align(Alignment.CenterHorizontally)) {
                    IconButton(
                        onClick = onDeleteClicked,
                        modifier = Modifier
                            .size(height = 30.dp, width = 30.dp)
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            painter = painterResource(Icons.Delete),
                            contentDescription = "Remove Query",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Tooltip("Query Type\n${item.type.pretty}", Modifier) {
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
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = chipColors[item.type.ordinal]),
                    )
                }
                Text(
                    text = "Accepted: ${item.nrAccepted()}",
                    style = androidx.compose.material.LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        lineHeight = 0.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both // Adjust this to change trimming behavior
                        )
                    ),
                    fontWeight = if (item.noNewAccepted) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            if (item.type != QueryType.EXPRESSION_SEARCH) {
                Text(
                    text = "Rejected: ${item.nrRejected()}\nStatus: ${item.status.value}\nLast expansion: ${
                        formatDateToyyyyMMMddFormat(
                            item.lastExpansionDate
                        )
                    }",
                    style = androidx.compose.material.LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        lineHeight = 0.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both // Adjust this to change trimming behavior
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            ElevatedButton(
                onClick = { (onGoClicked)(item.status.value, item.id) },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                elevation = ButtonDefaults.elevatedButtonElevation(6.dp),
            ) {
                Text(item.nextActionText())
            }
            Tooltip("View/Set flags on papers, export", Modifier.align(Alignment.CenterVertically)) {
                ElevatedButton(
                    onClick = onAnnotateClicked,
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                    elevation = ButtonDefaults.elevatedButtonElevation(4.dp),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    Text(
                        text = "Annotate\naccepted",
                        style = androidx.compose.material.LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            lineHeight = 0.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both // Adjust this to change trimming behavior
                            )
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(MARGIN_SCROLLBAR))
        }
    }
}
