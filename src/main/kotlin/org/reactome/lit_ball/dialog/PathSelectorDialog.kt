@file:Suppress("FunctionName")

package dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import window.components.Icons.Article
import window.components.Icons.Folder
import java.io.File

/**
 * A dialog for selecting a directory from the file system.
 * Written by Gemini 2.5 Pro, following directions by R. Stephan
 *
 * @param initialPath The starting path to display in the selector.
 * @param onPathSelected A callback function that is invoked when the user confirms a path selection.
 *                       The selected path is passed as a String.
 * @param onDismiss A callback function that is invoked when the dialog is dismissed.
 */
@Composable
fun PathSelectorDialog(
    initialPath: String,
    onPathSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // State for the currently displayed path. Initialized with the provided initialPath.
    var currentPath by remember { mutableStateOf(File(initialPath).absoluteFile) }
    val parentPath = currentPath.parentFile

    // Derived state to get the list of files and directories in the current path.
    // It automatically recalculates when `currentPath` changes.
    // The list is sorted to show directories first, then by name alphabetically.
    val filesAndDirs by derivedStateOf {
        currentPath.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }
    val listState = rememberLazyListState()
    val scrollbarAdapter = rememberScrollbarAdapter(scrollState = listState)
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Request focus on the dialog when it's first composed to enable keyboard navigation.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .height(500.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    // Handle keyboard events for navigation within the file list.
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            // Scroll down on down arrow press.
                            Key.DirectionDown -> {
                                coroutineScope.launch {
                                    listState.animateScrollBy(100f)
                                }
                                return@onPreviewKeyEvent true
                            }
                            // Scroll up on up arrow press.
                            Key.DirectionUp -> {
                                coroutineScope.launch {
                                    listState.animateScrollBy(-100f)
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        // Jump to the first directory starting with the pressed letter key.
                        val char = event.utf16CodePoint.toChar()
                        if (char.isLetter()) {
                            val index = filesAndDirs.indexOfFirst {
                                it.isDirectory && it.name.startsWith(char, ignoreCase = true)
                            }
                            if (index != -1) {
                                coroutineScope.launch {
                                    listState.scrollToItem(index)
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
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

                // Read-only text field to display the current absolute path.
                OutlinedTextField(
                    value = currentPath.absolutePath,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current Path") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "Go to parent directory" button, visible only if a parent exists.
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // The scrollable list of files and directories.
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
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
                                // Display a folder or file icon.
                                Icon(
                                    painter = if (file.isDirectory) painterResource(Folder) else painterResource(Article),
                                    contentDescription = if (file.isDirectory) "Directory" else "File",
                                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                // Display the file/directory name. Non-directories are dimmed.
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (file.isDirectory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                        }
                    }
                    // The scrollbar for the LazyColumn.
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = scrollbarAdapter
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Dialog action buttons (Cancel, Confirm).
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