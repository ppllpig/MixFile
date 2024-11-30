package com.donut.mixfile.ui.routes.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.appScope
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.file.InfoText
import com.donut.mixfile.util.file.saveFileToStorage
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.objects.AnimatedLoadingBar
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadTaskCard(
    downloadTask: DownloadTask,
    longClick: () -> Unit = {},
) {
    HorizontalDivider()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(107, 218, 246, 0),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    longClick()
                }
            ) {
                downloadTask.cancel()
            }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = downloadTask.fileName,
                color = colorScheme.primary,
                fontSize = 16.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "大小: ", value = formatFileSize(downloadTask.fileSize))
                downloadTask.State()
            }
            if (downloadTask.started && !downloadTask.stopped) {
                downloadTask.progress.LoadingContent()
            }
        }
    }
}

var downloadTasks by mutableStateOf(listOf<DownloadTask>())

var downloadSemaphore = Semaphore(3)

class DownloadTask(
    val fileName: String,
    val fileSize: Long,
    val url: String,
) {
    var progress = ProgressContent("下载中", 14.sp, colorScheme.secondary, false)

    var job: Job? = null


    var stopped by mutableStateOf(false)
    var started by mutableStateOf(false)

    var error: Throwable? by mutableStateOf(null)

    init {
        appScope.launch {
            downloadTasks += this@DownloadTask
        }
        totalDownloadFileCount++
        progress.contentLength = fileSize
    }

    @Composable
    fun State() {
        if (stopped) {
            if (error is CancellationException) {
                return Text(text = "下载取消", color = colorScheme.error)
            }
            if (error != null) {
                return Text(text = "下载失败", color = colorScheme.error)
            }
            return Text(text = "下载成功", color = colorScheme.primary)
        }
        if (started) {
            return Text(text = "下载中", color = colorScheme.primary)
        }
        return Text(text = "等待中", color = colorScheme.primary)
    }


    fun delete() {
        MixDialogBuilder("删除记录?").apply {
            setContent {
                Text(text = "文件: ${fileName}")
            }
            val currentError = error
            if (currentError != null) {
                setNegativeButton("查看错误信息") {
                    showErrorDialog(currentError, "错误信息")
                }
            }
            setPositiveButton("确定") {
                stop()
                downloadTasks -= this@DownloadTask
                closeDialog()
            }
            show()
        }
    }

    fun stop() {
        if (stopped) {
            return
        }
        job?.cancel("下载取消")
        stopped = true
    }

    fun start() {
        downloadQueue++
        job = appScope.launch(Dispatchers.IO) {
            downloadSemaphore.acquire()
            withContext(Dispatchers.Main) {
                downloadQueue--
            }
            if (stopped) {
                return@launch
            }
            started = true
            saveFileToStorage(
                url,
                displayName = fileName,
                progress = progress
            )
            downloadSuccessFileCount++
        }
        job?.invokeOnCompletion {
            error = it
            stopped = true
            downloadSemaphore.release()
        }

    }

    fun cancel() {
        if (stopped) {
            delete()
            return
        }
        MixDialogBuilder("取消下载?").apply {
            setContent {
                Text(text = "文件: ${fileName}")
            }
            setPositiveButton("确定") {
                stop()
                closeDialog()
                showToast("下载已取消")
            }
            show()
        }
    }
}

var downloadQueue by mutableIntStateOf(0)
var totalDownloadFileCount by mutableIntStateOf(0)
var downloadSuccessFileCount by mutableIntStateOf(0)

fun cancelAllDownloads() {
    downloadQueue = 0
    downloadTasks.forEach { it.stop() }
    totalDownloadFileCount = 0
    downloadSuccessFileCount = 0
}

fun showDownloadTaskWindow() {
    MixDialogBuilder("下载中的文件").apply {
        setContent {
            if (downloadTasks.isEmpty()) {
                Text(text = "没有下载中的文件")
                return@setContent
            }
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (totalDownloadFileCount > 1) {
                    val progress = downloadSuccessFileCount.toFloat() / totalDownloadFileCount
                    AnimatedLoadingBar(
                        progress = progress,
                        label = "总进度: $downloadSuccessFileCount/$totalDownloadFileCount " +
                                "正在下载: ${downloadTasks.filter { it.started && !it.stopped }.size} " +
                                "排队中: $downloadQueue"
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.padding(0.dp)
                ) {
                    downloadTasks.sortedBy {
                        if (it.started && !it.stopped) 0 else 1
                    }.take(5).forEach {
                        DownloadTaskCard(it) {
                            it.delete()
                        }
                    }
                }
            }
        }
        setPositiveButton("清除已完成任务") {
            downloadTasks = downloadTasks.filter { !it.stopped }
            showToast("清除成功")
        }
        setNegativeButton("全部取消") {
            showConfirmDialog("确定取消全部下载任务?") {
                cancelAllDownloads()
                showToast("取消成功")
            }
        }
        show()
    }
}

@Composable
fun DownloadDialogCard() {
    val downloading = downloadTasks.filter { !it.stopped }
    AnimatedVisibility(visible = downloading.isNotEmpty()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable {
                        showDownloadTaskWindow()
                    }
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloading.isNotEmpty()) {
                    Text(
                        text = "${downloading.size} 个文件正在下载中",
                        modifier = Modifier,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
