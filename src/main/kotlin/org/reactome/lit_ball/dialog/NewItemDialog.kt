@file:Suppress("FunctionName")
package org.reactome.lit_ball.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.QueryList


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NewItemDialog(
    rootScope: CoroutineScope,
    onCloseClicked: () -> Unit,
)
{
    val fieldValue = rememberSaveable { mutableStateOf( "") }
    val nameValue = rememberSaveable { mutableStateOf( "") }
    val checkValue = rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { (onCloseClicked)() },
        confirmButton = {
            TextButton(
                onClick = {
                    val dois = fieldValue.value.split("\n")
                        .map { it.trim() }
                        .toSet()
                    val name = nameValue.value.trim()
                    checkValue.value = dois.isNotEmpty() && name.isNotEmpty()
                    if (checkValue.value) {
                        rootScope.launch(Dispatchers.IO) {
                            QueryList.addNewItem(name, dois)
                        }
                        (onCloseClicked)()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { (onCloseClicked)() },
            ) {
                Text("Dismiss")
            }
        },
        title = {
            Text(
                "Create new query",
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                TextField(
                    value = nameValue.value,
                    onValueChange = { nameValue.value = it },
                    label = { Text("Query name") },
                )
                TextField(
                    value = fieldValue.value,
                    onValueChange = { fieldValue.value = it.uppercase() },
                    label = { Text("Core DOIs (one per line)") },
                    placeholder = { Text("10.XYZ/ABC\n10.XYZ/ABC") }
                )
                if (!checkValue.value)
                    Text("Please fill both text fields.")
            }
        },
    )
}