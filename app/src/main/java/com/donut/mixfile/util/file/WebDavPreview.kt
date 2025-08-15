package com.donut.mixfile.util.file

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.objects.isVideo
import com.donut.mixfile.server.core.objects.toDataLog
import com.donut.mixfile.server.core.routes.api.webdav.objects.WebDavManager
import com.donut.mixfile.server.core.routes.api.webdav.objects.normalizePath
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.webdav.importWebDavData
import com.donut.mixfile.util.AsyncEffect
import com.donut.mixfile.util.catchError
import com.donut.mixfile.util.compareByName
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.formatTime
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DavFolder(file: WebDavFile, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(107, 218, 246, 0))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = file.getName().trim(),
                    color = colorScheme.primary,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTime(file.lastModified),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun showWebDavFileList(manager: WebDavManager) {
    var currentPath by mutableStateOf("")
    var videoList: List<FileDataLog> by mutableStateOf(listOf())
    MixDialogBuilder("WebDAV存档预览").apply {
        setContent {
            if (currentPath.isNotEmpty()) {
                Text(
                    "当前目录: ${currentPath}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(5.dp, 10.dp)
                )
            }

            var fileList by remember {
                mutableStateOf(manager.listFiles(currentPath) ?: listOf())
            }

            AsyncEffect(currentPath) {
                catchError {
                    val files = manager.listFiles(currentPath) ?: listOf()
                    withContext(Dispatchers.Main) {
                        fileList = files
                    }
                    val sorted = files.sortedWith(
                        compareBy<WebDavFile> { !it.isFolder }
                            .thenComparing { file1, file2 ->
                                if (!isActive) {
                                    throw Exception("排序取消")
                                }
                                file1.getName().compareByName(file2.getName())
                            }
                    )
                    withContext(Dispatchers.Main) {
                        fileList = sorted
                    }
                }
            }

            LaunchedEffect(fileList) {
                videoList = fileList.map { it.toDataLog() }.filter {
                    it.isVideo
                }
            }
            BackHandler(enabled = currentPath.isNotEmpty()) {
                currentPath = currentPath.substringBeforeLast("/", "")
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(0.dp)
            ) {
                if (currentPath.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            currentPath = currentPath.substringBeforeLast("/", "")
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("返回上一级", color = colorScheme.primary)
                    }
                }
                if (fileList.isEmpty()) {
                    Text("没有文件")
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(0.dp, 1000.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(fileList.size) { index ->
                        val log = fileList[index]
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        if (log.isFolder) {
                            DavFolder(log) {
                                currentPath = normalizePath("${currentPath}/${log.getName()}")
                            }
                            return@items
                        }
                        FileCard(
                            log.toDataLog(),
                        )
                    }
                }
            }
        }
        setPositiveButton("导入存档") {
            showConfirmDialog("确定导入?") {
                importWebDavData(manager)
            }
        }
        setNegativeButton("全部播放") {
            if (videoList.isEmpty()) {
                showToast("没有视频文件")
                return@setNegativeButton
            }
            playVideoList(videoList)
        }
        show()
    }
}


fun previewWebDavData(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("解析中").apply {
        setContent {
            AsyncEffect {
                errorDialog("解析WebDav存档失败", onError = { closeDialog() }) {
                    val webDavDat = loadDataWithMaxSize(url, progress)
                    val manager = WebDavManager()
                    manager.loadDataFromBytes(webDavDat)
                    withContext(Dispatchers.Main) {
                        showWebDavFileList(manager)
                        closeDialog()
                    }
                }
            }
            progress.LoadingContent()
        }
        setDefaultNegative()
        show()
    }

}