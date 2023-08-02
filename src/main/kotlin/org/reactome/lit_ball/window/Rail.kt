@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun Rail(
    onRailItemClicked: List<() -> Unit>,
    onNewButtonClicked: () -> Unit,
    onExit: () -> Unit,
) {
    val selectedItem by remember { mutableStateOf(0) }

    data class RailItem(
        val text: String,
        val icon: ImageVector,
        val actionIndex: Int,
        val extraAction: (() -> Unit)? = null
    )

    val items = listOf(
        RailItem("Info", Icons.Filled.Info, 0),
        RailItem("Settings", Icons.Filled.Settings, 1),
        RailItem("Exit", Icons.Filled.ExitToApp, 3, onExit)
    )

    NavigationRail {
        items.forEach { item ->
            NavigationRailItem(
                onClick = {
                    onRailItemClicked[item.actionIndex]()
                    item.extraAction?.invoke()
                },
                icon = { Icon(item.icon, null) },
                label = { Text(item.text) },
                selected = selectedItem == item.actionIndex
            )
            ExtendedFloatingActionButton(
                onClick = onNewButtonClicked,
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                text = {
                    Text("New Query")
                },
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 36.dp)
            )
        }
    }
}