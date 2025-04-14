package window.components

data class RailItem(
    val text: String,
    val tooltipText: String = "",
    val iconPainterResource: String,
    val actionIndex: Int,
    val extraAction: (() -> Unit)? = null,
    val onClicked: () -> Unit,
)


