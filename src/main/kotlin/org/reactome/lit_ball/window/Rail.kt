@file:Suppress("FunctionName")

package org.reactome.lit_ball.window

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Rail(
    railItems: List<RailItem>,
    onNewButtonClicked: () -> Unit,
    rootSwitch: MutableState<Boolean>,
    ) {
    val selectedItem by remember { mutableStateOf(0) }

    NavigationRail {
        railItems.forEach { item ->
            NavigationRailItem(
                onClick = {
                    item.onClicked.invoke()
                    item.extraAction?.invoke()
                },
                icon = {
                    Icon(
                        item.icon,
                        null,
                    ) },
                label = {
                    Text(item.text,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) },
                selected = selectedItem == item.actionIndex
            )
        }
        if (!rootSwitch.value) {
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