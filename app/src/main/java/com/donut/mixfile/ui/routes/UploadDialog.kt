package com.donut.mixfile.ui.routes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.home.UploadTaskCard
import com.donut.mixfile.ui.routes.home.uploadTasks
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.file.cancelAllMultiUpload
import com.donut.mixfile.util.file.totalUploadFileCount
import com.donut.mixfile.util.file.uploadQueue
import com.donut.mixfile.util.file.uploadSuccessFileCount
import com.donut.mixfile.util.objects.AnimatedLoadingBar
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showToast


fun showUploadTaskWindow() {
    MixDialogBuilder("上传任务").apply {
        setContent {
            if (uploadTasks.isEmpty()) {
                Text(text = "没有上传中的文件")
                return@setContent
            }
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (totalUploadFileCount > 1) {
                    val progress = uploadSuccessFileCount.toFloat() / totalUploadFileCount
                    AnimatedLoadingBar(
                        progress = progress,
                        label = "总进度: ${uploadSuccessFileCount}/${totalUploadFileCount} " +
                                "正在上传: ${uploadTasks.filter { it.uploading }.size} " +
                                "排队中: $uploadQueue"
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.padding(0.dp)
                ) {
                    uploadTasks.take(10).forEach {
                        UploadTaskCard(uploadTask = it) {
                            it.delete()
                        }
                    }
                }
            }
        }
        setPositiveButton("清除失败任务") {
            uploadTasks = uploadTasks.filter { !it.stopped }
            showToast("清除成功")
        }
        setNegativeButton("全部取消") {
            showConfirmDialog("确定取消全部上传任务?") {
                cancelAllMultiUpload()
                showToast("取消成功")
            }
        }
        show()
    }
}

@Composable
fun UploadDialogCard() {
    AnimatedVisibility(visible = uploadTasks.isNotEmpty()) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable {
                        showUploadTaskWindow()
                    }
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val uploading = uploadTasks.filter { it.uploading }.size
                if (uploading > 0) {
                    Text(
                        text = "$uploading 个文件正在上传中",
                        modifier = Modifier,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    val failed = uploadTasks.filter { !it.uploading }.size
                    if (failed > 0) {
                        Text(
                            text = "$failed 个文件上传失败",
                            modifier = Modifier,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}