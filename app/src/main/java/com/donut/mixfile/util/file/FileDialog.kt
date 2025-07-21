package com.donut.mixfile.util.file

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.donut.mixfile.activity.video.VideoActivity
import com.donut.mixfile.app
import com.donut.mixfile.currentActivity
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.isImage
import com.donut.mixfile.server.core.objects.isVideo
import com.donut.mixfile.server.core.utils.extensions.isNotNull
import com.donut.mixfile.server.core.utils.hashSHA256
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.core.utils.shareCode
import com.donut.mixfile.server.core.utils.toHex
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.favorites.openCategorySelect
import com.donut.mixfile.ui.routes.home.DownloadTask
import com.donut.mixfile.ui.routes.home.showDownloadTaskWindow
import com.donut.mixfile.ui.routes.settings.useShortCode
import com.donut.mixfile.ui.routes.settings.useSystemPlayer
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.CachedDelegate
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.showToast

@Composable
fun FileChip(text: String, operation: () -> Unit) {
    AssistChip(onClick = operation, label = {
        Text(text = text, color = colorScheme.primary)
    })
}

@OptIn(ExperimentalLayoutApi::class)
fun showFileInfoDialog(
    dataLog: FileDataLog,
    onDismiss: () -> Unit = {}
) {
    var isFav = false

    val log by CachedDelegate({ arrayOf(favorites) }) {
        val firstSimilar = favorites.firstOrNull { it.isSimilar(dataLog) }
        isFav = firstSimilar.isNotNull()
        firstSimilar ?: dataLog
    }

    val shareInfo = resolveMixShareInfo(log.shareInfoData)
    if (shareInfo == null) {
        showToast("解析文件分享码失败")
        return
    }

    MixDialogBuilder("文件信息", tag = "file-info-${shareInfo.url}").apply {
        onDismiss(onDismiss)
        setNegativeButton("复制分享码") {
            shareInfo.shareCode(useShortCode).copyToClipboard()
        }
        setContent {
            val fileName = log.name
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "名称: ", value = fileName)
                InfoText(key = "大小: ", value = formatFileSize(shareInfo.fileSize))
                InfoText(key = "密钥: ", value = shareInfo.key)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (fileName.endsWith(".mix_list")) {
                        FileChip("文件列表") {
                            importFileList(log.downloadUrl)
                        }
                    }
                    if (fileName.endsWith(".mix_dav")) {
                        FileChip("查看文件") {
                            previewWebDavData(log.downloadUrl)
                        }
                    }
                    if (!isFav) {
                        FileChip("收藏") {
                            addFavoriteLog(log)
                        }
                    } else {
                        FileChip("取消收藏") {
                            deleteFavoriteLog(log)
                        }
                        FileChip("重命名") {
                            log.rename {
                                closeDialog()
                                showFileInfoDialog(it, onDismiss)
                            }
                        }
                        FileChip("分类: ${log.getCategory()}") {
                            openCategorySelect(log.getCategory()) { category ->
                                favorites = log.updateDataList(favorites) {
                                    log.copy(category = category)
                                }
                            }
                        }
                    }
                    if (dataLog.isVideo) {
                        FileChip("播放视频") {
                            if (useSystemPlayer) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(log.downloadUrl.toUri(), "video/*")
                                currentActivity?.startActivity(intent)
                                return@FileChip
                            }
                            val intent = Intent(app, VideoActivity::class.java).apply {
                                putExtra("url", log.downloadUrl)
                                putExtra("hash", shareInfo.toString().hashSHA256().toHex())
                            }
                            currentActivity?.startActivity(intent)
                        }
                    }
                    if (dataLog.isImage) {
                        FileChip("查看图片") {
                            showImageDialog(log.downloadUrl)
                        }
                    }
                    FileChip("复制局域网地址") {
                        log.lanUrl.copyToClipboard()
                    }
                }
            }
        }
        setPositiveButton("下载文件") {
            downloadFile(log)
            closeDialog()
            showDownloadTaskWindow()
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

fun downloadFile(file: FileDataLog) {
    val task = DownloadTask(file.name, file.size, file.downloadUrl)
    task.start()
}
