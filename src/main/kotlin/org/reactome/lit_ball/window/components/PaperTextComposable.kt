package window.components

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun paperTextComposable(cardTitle: String?, modifier: Modifier) {
    Text(
        text = cardTitle ?: "",
        style = LocalTextStyle.current.copy(
            fontSize = 12.sp,
            lineHeight = 0.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both // Adjust this to change trimming behavior
            )
        ),
        fontWeight = FontWeight.Bold,
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
