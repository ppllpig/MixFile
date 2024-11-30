package com.donut.mixfile.ui.routes.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.appScope
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.file.InfoText
import com.donut.mixfile.util.file.addUploadLog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun UploadTaskCard(
    uploadTask: UploadTask,
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
                if (uploadTask.result.isNotEmpty()) {
                    tryResolveFile(uploadTask.result)
                    return@combinedClickable
                }
                uploadTask.cancel()
            }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = uploadTask.fileName,
                color = colorScheme.primary,
                fontSize = 16.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "大小: ", value = formatFileSize(uploadTask.fileSize))
                uploadTask.State()
            }
            if (uploadTask.uploading) {
                uploadTask.progress.LoadingContent()
            }
        }
    }
}


var uploadTasks by mutableStateOf(listOf<UploadTask>())

class UploadTask(
    val call: ApplicationCall? = null,
    val fileName: String,
    val fileSize: Long,
    val add: Boolean = true,
) {
    var progress = ProgressContent("上传中", 14.sp, colorScheme.secondary, false)

    var onStop = {}


    var stopped by mutableStateOf(false)

    var error: Throwable? by mutableStateOf(null)

    var result by mutableStateOf("")
        private set

    val uploading
        get() = result.isEmpty() && !stopped

    init {
        appScope.launch {
            uploadTasks += this@UploadTask
        }
        progress.contentLength = fileSize
    }

    @Composable
    fun State() {
        if (stopped) {
            if (error == null || error is CancellationException) {
                return Text(text = "上传取消", color = colorScheme.error)
            }
            return Text(text = "上传失败", color = colorScheme.error)
        }
        Text(text = "上传中", color = colorScheme.primary)
    }


    suspend fun complete(shareInfo: MixShareInfo) {
        withContext(Dispatchers.Main) {
            result = shareInfo.toString()
            uploadTasks -= this@UploadTask
            if (add) {
                addUploadLog(shareInfo)
            }
        }
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
                uploadTasks -= this@UploadTask
                closeDialog()
            }
            show()
        }
    }

    fun stop() {
        if (stopped) {
            return
        }
        stopped = true
        appScope.launch(Dispatchers.IO) {
            call?.respondText("上传已取消", status = HttpStatusCode.InternalServerError)
            onStop()
        }
    }

    fun cancel() {
        if (stopped) {
            delete()
            return
        }
        MixDialogBuilder("取消上传?").apply {
            setContent {
                Text(text = "文件: ${fileName}")
            }
            setPositiveButton("确定") {
                stop()
                closeDialog()
                showToast("上传已取消")
            }
            show()
        }
    }


}