package com.donut.mixfile.ui.routes.webdav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.util.file.formatFileSize

@Composable
fun WebDavRestoreDialog(onClose: () -> Unit, vm: WebDavRestoreViewModel = viewModel()) {

    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }

    val isLoading by vm.isLoading.collectAsState()
    val fileList by vm.fileList.collectAsState()
    val currentPath by vm.currentPath.collectAsState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!connected) {
                    Text("Connect to WebDAV", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(value = url, onValueChange = { url = it }, label = { Text("WebDAV URL") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        vm.listFiles(url, user, pass)
                        connected = true
                    }, enabled = !isLoading && url.isNotBlank()) {
                        Text("Connect")
                    }
                } else {
                    // File Browser UI
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.navigateBack(url, user, pass) }, enabled = currentPath != "/") {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Text(currentPath, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(fileList) { file ->
                                FileRow(file = file,
                                    isSelected = vm.selectedFiles.contains(file),
                                    onSelect = { vm.toggleSelection(file) },
                                    onNavigate = { vm.navigateTo(file, url, user, pass) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onClose) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            vm.startRestore(url, user, pass)
                            onClose()
                        }, enabled = vm.selectedFiles.isNotEmpty()) {
                            Text("Restore Selected")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: WebDavFile, isSelected: Boolean, onSelect: () -> Unit, onNavigate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (file.isFolder) onNavigate() else onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.getName(), maxLines = 1)
            if (!file.isFolder) {
                Text(formatFileSize(file.size), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!file.isFolder) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
        }
    }
}