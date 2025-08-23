package com.donut.mixfile.ui.routes.webdav

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.donut.mixfile.server.FileService
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.util.showError
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebDavRestoreViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _fileList = MutableStateFlow<List<WebDavFile>>(emptyList())
    val fileList = _fileList.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath = _currentPath.asStateFlow()

    val selectedFiles = mutableStateListOf<WebDavFile>()

    fun listFiles(url: String, user: String, pass: String, path: String = "/") {
        selectedFiles.clear() // Clear selection when listing new files
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            try {
                val files = FileService.instance?.listRemoteWebDavFiles(url, path, user, pass)
                _fileList.value = files ?: emptyList()
            } catch (e: Exception) {
                showError(e)
                _fileList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateTo(folder: WebDavFile, url: String, user: String, pass: String) {
        if (!folder.isFolder) return
        val newPath = (_currentPath.value.removeSuffix("/") + "/" + folder.getName()).let {
            if (it.startsWith("//")) it.substring(1) else it
        }
        listFiles(url, user, pass, newPath)
    }

    fun navigateBack(url: String, user: String, pass: String) {
        if (_currentPath.value == "/") return
        val parentPath = _currentPath.value.substringBeforeLast('/', "/")
        listFiles(url, user, pass, if (parentPath.isEmpty()) "/" else parentPath)
    fun toggleSelection(file: WebDavFile) {
        // Allow selecting only one file
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
        } else {
            selectedFiles.clear()
            selectedFiles.add(file)
        }
    }

    fun startRestore(url: String, user: String, pass: String) {
        val fileToRestore = selectedFiles.firstOrNull()
        if (fileToRestore == null) {
            showToast("Please select a .mix_dav file to restore.")
            return
        }

        if (fileToRestore.isFolder || !fileToRestore.getName().endsWith(".mix_dav")) {
            showToast("Invalid file. Please select a .mix_dav archive.")
            return
        }

        FileService.instance?.startRestoreFromArchive(
            file = fileToRestore,
            sourceUrl = url,
            sourceUser = user,
            sourcePass = pass,
            targetPath = _currentPath.value
        )
        showToast("Restore started in background.")
        selectedFiles.clear()
    }
}
    }
}