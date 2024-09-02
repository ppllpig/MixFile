package com.donut.mixfile.util.file

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.donut.mixfile.MainActivity
import com.donut.mixfile.activity.VideoActivity
import com.donut.mixfile.app
import com.donut.mixfile.server.StreamContent
import com.donut.mixfile.server.localClient
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.getLocalServerAddress
import com.donut.mixfile.ui.routes.tryResolveFile
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getFileName
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext


@SuppressLint("Recycle")
fun selectAndUploadFile() {
    MainActivity.mixFileSelector.openSelect { uri ->
        MixDialogBuilder(
            "上传中", properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ).apply {
            setContent {
                val progressContent = remember {
                    ProgressContent("上传中")
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    progressContent.LoadingContent()
                }
                UseEffect {
                    val resolver = app.contentResolver
                    val fileDescriptor: AssetFileDescriptor? =
                        resolver.openAssetFileDescriptor(uri, "r")
                    val fileSize = fileDescriptor?.length ?: 0
                    errorDialog("上传失败") {
                        val response = localClient.put {
                            timeout {
                                requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
                            }
                            url("${getLocalServerAddress()}/api/upload")
                            onUpload(progressContent.ktorListener)
                            val fileStream = resolver.openInputStream(uri)
                            if (fileStream == null) {
                                showToast("打开文件失败")
                                return@put
                            }
                            parameter("name", uri.getFileName())
                            setBody(StreamContent(fileStream, fileSize))
                        }
                        val message = response.bodyAsText()
                        if (!response.status.isSuccess()) {
                            throw Exception("上传失败: $message")
                        }
                        withContext(Dispatchers.Main) {
                            tryResolveFile(message)
                        }
                        showToast("上传成功!")
                    }
                    closeDialog()
                }
            }
            setNegativeButton("取消") {
                showToast("上传已取消")
                closeDialog()
            }
            show()
        }
    }
}

suspend fun saveFileToStorage(
    url: String,
    displayName: String,
    progress: ProgressContent,
    directory: String = Environment.DIRECTORY_DOWNLOADS,
    storeUri: Uri = MediaStore.Files.getContentUri("external"),
): Uri? {
    val resolver = app.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//        put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
        put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
    }


    val fileUri = resolver.insert(storeUri, contentValues)
    coroutineContext.job.invokeOnCompletion { throwable ->
        if (throwable !is CancellationException) {
            return@invokeOnCompletion
        }
        if (fileUri == null) {
            return@invokeOnCompletion
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resolver.delete(fileUri, null)
        }
    }
    if (fileUri == null) {
        return null
    }
    localClient.prepareGet {
        timeout {
            requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
        }
        url(url)
        onDownload(progress.ktorListener)
    }.execute {
        if (!it.status.isSuccess()) {
            val text = if ((it.contentLength()
                    ?: (1024 * 1024)) < 1024 * 500
            ) it.bodyAsText() else "未知错误"
            throw Exception("下载失败: ${text}")
        }
        resolver.openOutputStream(fileUri)?.use { output ->
            it.bodyAsChannel().toInputStream().use { input ->
                input.copyTo(output)
            }
        }
    }
    return fileUri
}


@OptIn(ExperimentalLayoutApi::class)
fun showFileShareDialog(shareInfo: MixShareInfo, onDismiss: () -> Unit = {}) {
    MixDialogBuilder("文件信息").apply {
        onDismiss(onDismiss)
        setContent {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "名称: ", value = shareInfo.fileName)
                InfoText(key = "大小: ", value = formatFileSize(shareInfo.fileSize))
                InfoText(key = "密钥: ", value = shareInfo.key)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(onClick = {
                        shareInfo.shareCode().copyToClipboard()
                    }, label = {
                        Text(text = "复制分享码", color = colorScheme.primary)
                    })
                    if (!isFavorite(shareInfo)) {
                        AssistChip(onClick = {
                            addFavoriteLog(shareInfo)
                        }, label = {
                            Text(text = "收藏", color = colorScheme.primary)
                        })
                    } else {
                        AssistChip(onClick = {
                            deleteFavoriteLog(shareInfo.toDataLog())
                        }, label = {
                            Text(text = "取消收藏", color = colorScheme.primary)
                        })
                    }

                    if (shareInfo.contentType().startsWith("video/")) {
                        AssistChip(onClick = {
                            closeDialog()
                            val intent = Intent(app, VideoActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("url", shareInfo.downloadUrl)
                            }
                            app.startActivity(intent)
                        }, label = {
                            Text(text = "播放视频", color = colorScheme.primary)
                        })
                    }
                    if (shareInfo.contentType().startsWith("image/")) {
                        AssistChip(onClick = {
                            showImageDialog(shareInfo.downloadUrl)
                        }, label = {
                            Text(text = "查看图片", color = colorScheme.primary)
                        })
                    }

                    AssistChip(onClick = {
                        closeDialog()
                        shareInfo.lanUrl.copyToClipboard()
                    }, label = {
                        Text(text = "复制局域网地址", color = colorScheme.primary)
                    })
                }
            }
        }
        setPositiveButton("下载文件") {
            closeDialog()
            downloadFile(shareInfo)
        }
        show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoText(key: String, value: String) {
    FlowRow {
        Text(text = key, fontSize = 14.sp, color = Color(117, 115, 115, 255))
        Text(
            text = value,
            color = colorScheme.primary.copy(alpha = 0.8f),
            textDecoration = TextDecoration.Underline,
            fontSize = 14.sp,
        )
    }
}

fun downloadFile(shareInfo: MixShareInfo) {
    MixDialogBuilder(
        "文件下载中", properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ).apply {
        setContent {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val progressContent = remember {
                    ProgressContent()
                }
                progressContent.LoadingContent()

                UseEffect {
                    errorDialog("下载失败") {
                        saveFileToStorage(
                            shareInfo.downloadUrl,
                            displayName = shareInfo.fileName,
                            progress = progressContent
                        )
                        showToast("文件已保存到下载目录")
                    }
                    closeDialog()
                }
            }
        }
        setNegativeButton("取消") {
            showToast("下载已取消")
            closeDialog()
        }
        show()
    }
}

fun resolveMixShareInfo(value: String): MixShareInfo? {
    return parseShareCode(value)
}