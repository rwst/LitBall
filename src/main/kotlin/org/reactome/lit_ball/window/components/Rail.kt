@file:Suppress("FunctionName")

package window.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import window.RootType

@Composable
fun Rail(
    railItems: List<RailItem>,
    onNewButtonClicked: () -> Unit,
    rootSwitch: MutableState<RootType>,
) {
    val selectedItem by remember { mutableStateOf(0) }

    NavigationRail {
        Image(
            painterResource(window.components.Icons.Logo),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        railItems.forEach { item ->
            Tooltip(item.tooltipText, Modifier.align(Alignment.CenterHorizontally)) {
                NavigationRailItem(
                    onClick = {
                        runBlocking {
                            item.onClicked.invoke()
                            item.extraAction?.invoke()
                        }
                    },
                    icon = {
                        Icon(
                            painterResource(item.iconPainterResource),
                            null,
                        )
                    },
                    label = {
                        Text(
                            item.text,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    },
                    selected = selectedItem == item.actionIndex
                )
            }
        }
        if (rootSwitch.value == RootType.MAIN_ROOT) {
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