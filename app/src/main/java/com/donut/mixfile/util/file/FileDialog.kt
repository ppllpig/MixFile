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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.donut.mixfile.activity.VideoActivity
import com.donut.mixfile.app
import com.donut.mixfile.currentActivity
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.favorites.importFileList
import com.donut.mixfile.ui.routes.favorites.openCategorySelect
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast

@OptIn(ExperimentalLayoutApi::class)
fun showFileInfoDialog(shareInfo: MixShareInfo, onDismiss: () -> Unit = {}) {
    MixDialogBuilder("文件信息").apply {
        onDismiss(onDismiss)
        setContent {
            val dataLog = remember(shareInfo, favorites, uploadLogs) {
                val log = shareInfo.toDataLog()
                favorites.firstOrNull { it.shareInfoData == log.shareInfoData } ?: log
            }
            val fileName = dataLog.name
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "名称: ", value = fileName)
                InfoText(key = "大小: ", value = formatFileSize(shareInfo.fileSize))
                InfoText(key = "密钥: ", value = shareInfo.key)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(onClick = {
                        shareInfo.shareCode().copyToClipboard()
                    }, label = {
                        Text(text = "复制分享码", color = colorScheme.primary)
                    })
                    if (fileName.startsWith("__mixfile_list") || fileName.endsWith(".mix_list")) {
                        AssistChip(onClick = {
                            importFileList(shareInfo.downloadUrl)
                        }, label = {
                            Text(text = "文件列表", color = colorScheme.primary)
                        })
                    }
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
                        AssistChip(onClick = {
                            dataLog.rename {
                                closeDialog()
                                showFileInfoDialog(it)
                            }
                        }, label = {
                            Text(text = "重命名", color = colorScheme.primary)
                        })
                        AssistChip(onClick = {
                            openCategorySelect(dataLog.category) { category ->
                                favorites = dataLog.updateDataList(favorites) {
                                    dataLog.copy(category = category)
                                }
                            }
                        }, label = {
                            Text(
                                text = "分类: ${dataLog.category}",
                                color = colorScheme.primary
                            )
                        })

                    }

                    if (shareInfo.contentType().startsWith("video/")) {
                        AssistChip(onClick = {
                            val intent = Intent(app, VideoActivity::class.java).apply {
                                putExtra("url", shareInfo.downloadUrl)
                            }
                            currentActivity.startActivity(intent)
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
                        shareInfo.lanUrl.copyToClipboard()
                    }, label = {
                        Text(text = "复制局域网地址", color = colorScheme.primary)
                    })
                }
            }
        }
        setPositiveButton("下载文件") {
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
