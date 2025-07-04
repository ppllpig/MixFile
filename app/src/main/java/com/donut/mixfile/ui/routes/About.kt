package com.donut.mixfile.ui.routes

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.donut.mixfile.app
import com.donut.mixfile.currentActivity
import com.donut.mixfile.server.core.utils.ignoreError
import com.donut.mixfile.ui.component.common.CommonSwitch
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.updateChecker
import com.donut.mixfile.util.AsyncEffect
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getAppVersionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var uploadedDataBytes by cachedMutableOf(0, "uploaded_data_bytes")
var downloadedDataBytes by cachedMutableOf(0, "downloaded_data_bytes")
var autoCheckUpdate by cachedMutableOf(true, "auto_check_update")

@Synchronized
fun increaseUploadData(size: Long) {
    uploadedDataBytes += size
}

@Synchronized
fun increaseDownloadData(size: Long) {
    downloadedDataBytes += size
}

@OptIn(ExperimentalLayoutApi::class)
val About = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    OutlinedCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = "统计",
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "已上传数据: ",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = formatFileSize(uploadedDataBytes),
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "已下载数据: ",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = formatFileSize(downloadedDataBytes),
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            OutlinedButton(onClick = {
                MixDialogBuilder("确定重置统计?").apply {
                    setPositiveButton("确定") {
                        uploadedDataBytes = 0
                        downloadedDataBytes = 0
                        it()
                    }
                    show()
                }
            }) {
                Text(text = "重置")
            }
        }
    }

    OutlinedCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = "当前版本: ${getAppVersionName(LocalContext.current)}",
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                color = colorScheme.primary,
                text = "项目地址: https://github.com/InvertGeek/MixFile",
                modifier = Modifier.clickable {
                    openGithubLink()
                }
            )

            OutlinedButton(onClick = {
                showUpdateDialog()
            }) {
                Text(text = "检查更新")
            }
            CommonSwitch(
                checked = autoCheckUpdate,
                text = "启动时自动检查更新:"
            ) {
                autoCheckUpdate = it
            }

        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            color = Color.Gray,
            text = """
        MixFile采用混合式文件加密上传系统
        您上传的所有文件都会使用AES-GCM算法加密,
        上传时会生成随机的256位密钥在本地进行加密后进行上传,
        只要不泄漏分享码,文件内容是无法被任何人得知的
        256位密钥可确保即使是在未来使用量子计算机也无法破解
        分享码中包含了,文件地址，文件大小,加密使用的密钥等信息
        分享码默认使用不可见字符隐写到普通文本中进行编码信息
        如果长度超过发送限制可关闭使用短分享码功能
        如果分享码丢失，上传的文件将永远无法找回
        请把应用省电改为无限制,否则文件服务器可能无法在后台运行
        开启自启动权限后,关闭主页面时将会自动在后台运行服务器
    """.trimIndent()
        )
    }
}

fun openGithubLink() {
    val intent =
        Intent(
            Intent.ACTION_VIEW,
            "https://github.com/InvertGeek/MixFile".toUri()
        )
    currentActivity?.startActivity(intent)
}

suspend fun checkForUpdates(latest: String? = null, showUpdatedDialog: Boolean = false) {
    val latestVersion =
        latest ?: withContext(Dispatchers.IO) { ignoreError { updateChecker.latestVersion } }
    if (latestVersion == null) {
        return
    }
    if (latestVersion.contentEquals(getAppVersionName(app))) {
        if (showUpdatedDialog) {
            MixDialogBuilder("已是最新版本").apply {
                setPositiveButton("确定") { closeDialog() }
                show()
            }
        }
        return
    }
    MixDialogBuilder("有新版本: ${latestVersion}").apply {
        setPositiveButton("下载") { openGithubLink() }
        show()
    }
}

fun showUpdateDialog() {
    MixDialogBuilder("检查更新中").apply {
        setContent {
            var latestVersion: String? by remember { mutableStateOf(null) }

            AsyncEffect {
                ignoreError {
                    latestVersion = updateChecker.latestVersion
                }
                withContext(Dispatchers.Main) {
                    closeDialog()
                    checkForUpdates(latestVersion, showUpdatedDialog = true)
                }
            }
            if (latestVersion == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }

                return@setContent
            }
        }
        setDefaultNegative()
        show()
    }
}