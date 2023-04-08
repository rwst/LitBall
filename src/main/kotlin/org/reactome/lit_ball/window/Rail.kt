@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Rail(
    onRailItemClicked: List<() -> Unit>,
    onNewButtonClicked: () -> Unit
) {
    val selectedItem by remember { mutableStateOf(0) }
//    val items = listOf("Info", "Settings", "Exit")
//    val icons = listOf(Icons.Filled.Settings, Icons.Filled.ExitToApp)
//    val actions = listOf( {  } , { App.buttonSettings() }, { App.buttonExit() } )
    NavigationRail {
        NavigationRailItem(
            onClick = onRailItemClicked[0],
            icon = { Icon(Icons.Filled.Info, null) },
            label = { Text("Info") },
            selected = selectedItem == 1
        )
        NavigationRailItem(
            onClick = onRailItemClicked[1],
            icon = { Icon(Icons.Filled.Settings, null) },
            label = { Text("Settings") },
            selected = selectedItem == 2
        )
        NavigationRailItem(
            onClick = onRailItemClicked[2],
            icon = { Icon(Icons.Filled.ExitToApp, null) },
            label = { Text("Exit") },
            selected = selectedItem == 3
        )
        ExtendedFloatingActionButton(onClick = onNewButtonClicked,
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
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 36.dp))
    }
}
