@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.reactome.lit_ball.common.App

@Composable
fun LitBallRail() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Info", "Settings", "Exit")
    val icons = listOf(Icons.Filled.Info, Icons.Filled.Settings, Icons.Filled.ExitToApp)
    val actions = listOf( { App.buttonInfo() } , { App.buttonSettings() }, { App.buttonExit() } )
    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                onClick = {
                    selectedItem = index
                    actions[index]()
                          },
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index
            )
        }
        ExtendedFloatingActionButton(onClick = { App.buttonNew() },
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