@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.reactome.lit_ball.common.App

@Composable
fun LitBallRail() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)
    val actions = listOf( { App.buttonHome() } , { App.buttonSearch() }, { App.buttonSettings() } )
    NavigationRail {
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
            })
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
    }
}