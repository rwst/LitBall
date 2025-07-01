@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun PathSelectorDialog(
    initialPath: String,
    onPathSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentPath by remember { mutableStateOf(File(initialPath).absoluteFile) }
    val parentPath = currentPath.parentFile

    val filesAndDirs by derivedStateOf {
        currentPath.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .height(500.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select a Directory",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = currentPath.absolutePath,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current Path") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (parentPath != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentPath = parentPath }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go to parent directory")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("..")
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filesAndDirs) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = file.isDirectory) {
                                    if (file.isDirectory) {
                                        currentPath = file
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Filled.Email else Icons.Filled.Info,
                                contentDescription = if (file.isDirectory) "Directory" else "File",
                                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (file.isDirectory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onPathSelected(currentPath.absolutePath)
                            onDismiss()
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}