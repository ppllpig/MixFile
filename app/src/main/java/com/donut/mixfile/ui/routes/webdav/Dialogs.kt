package com.donut.mixfile.ui.routes.webdav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.donut.mixfile.kv
import com.donut.mixfile.server.WEB_DAV_KEY
import com.donut.mixfile.server.core.routes.api.webdav.utils.WebDavFile
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.mixFileServer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.file.doUploadFile
import com.donut.mixfile.util.file.loadFileList
import com.donut.mixfile.util.file.localClient
import com.donut.mixfile.util.file.toDataLog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getCurrentTime
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

fun clearWebDavData() {
    MixDialogBuilder("确定清空webdav数据?").apply {
        var text by mutableStateOf("")
        setContent {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "请输入确认")
                },
                maxLines = 1,

                )
        }
        setDefaultNegative()
        setPositiveButton("确认") {
            if (!text.contentEquals("确认")) {
                showToast("请输入确认")
                return@setPositiveButton
            }
            mixFileServer.webDav.WEBDAV_DATA.clear()
            kv.remove(WEB_DAV_KEY)
            showToast("数据已清空")
            closeDialog()
        }
        show()
    }
}

fun exportWebDavData() {
    MixDialogBuilder("确定导出?").apply {
        var fileName by mutableStateOf("webdav存档-${getCurrentTime()}")
        setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = "存档备注")
                    },
                    maxLines = 1,
                )
                Text(text = "将会导出当前webdav存档")
            }
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            doUploadFile(
                mixFileServer.webDav.dataToBytes(),
                "${fileName}.mix_dav",
                false
            )
            closeDialog()
        }
        show()
    }
}

suspend fun loadWebDavData(url: String, progressContent: ProgressContent): ByteArray? {
    try {
        return localClient.prepareGet {
            url(url)
            onDownload(progressContent.ktorListener)
        }.execute {
            if (!it.status.isSuccess()) {
                val text = if ((it.contentLength()
                        ?: (1024 * 1024)) < 1024 * 500
                ) it.bodyAsText() else "未知错误"
                throw Exception("下载失败: ${text}")
            }
            if ((it.contentLength() ?: 0) > 1024 * 1024 * 50) {
                throw Exception("文件过大")
            }
            val data = it.bodyAsChannel().readRemaining(1024 * 1024 * 50).readByteArray()
            return@execute data
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            showErrorDialog(e, "解析分享列表失败!")
        }
    }
    return null
}

fun importFileListToWebDav(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("解析中").apply {
        setContent {
            UseEffect {
                val fileList = loadFileList(url, progress)
                if (fileList == null) {
                    showToast("解析分享列表失败!")
                    closeDialog()
                    return@UseEffect
                }
                fileList.forEach {
                    //创建分类文件夹
                    mixFileServer.webDav.addFileNode(
                        "",
                        WebDavFile(it.category, isFolder = true)
                    )
                    mixFileServer.webDav.addFileNode(
                        it.category,
                        WebDavFile(it.name, shareInfoData = it.shareInfoData, size = it.size)
                    )
                }
                mixFileServer.webDav.saveData()
                showToast("导入完成")
                withContext(Dispatchers.Main) {
                    closeDialog()
                }
            }
            progress.LoadingContent()
        }
        setDefaultNegative()
        show()
    }

}

fun tryImportWebDavData(code: String) {
    val shareInfo = resolveMixShareInfo(code)
    if (shareInfo == null) {
        showToast("解析分享码失败")
        return
    }
    val url = shareInfo.toDataLog().downloadUrl
    if (shareInfo.fileName.endsWith(".mix_list")) {
        MixDialogBuilder("确定导入文件列表?").apply {
            setContent {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "文件列表: ${shareInfo.fileName}",
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("大小: ${formatFileSize(shareInfo.fileSize)}", color = colorScheme.primary)
                }
            }
            setPositiveButton("确定") {
                closeDialog()
                importFileListToWebDav(url)
            }
            setDefaultNegative()
            show()
        }
        return
    }
    if (!shareInfo.fileName.endsWith(".mix_dav")) {
        showToast("错误的文件格式")
        return
    }
    MixDialogBuilder("确定导入存档?").apply {
        setContent {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "存档: ${shareInfo.fileName}",
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("大小: ${formatFileSize(shareInfo.fileSize)}", color = colorScheme.primary)
            }
        }
        setPositiveButton("确定") {
            closeDialog()
            importWebDavData(url)
        }
        setDefaultNegative()
        show()
    }
}

fun importWebDavData(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("导入中").apply {
        setContent {
            UseEffect {
                val webDavData = loadWebDavData(url, progress)
                if (webDavData == null) {
                    showToast("下载文件失败!")
                    closeDialog()
                    return@UseEffect
                }
                val dav = mixFileServer.webDav
                val data = dav.parseDataFromBytes(webDavData)
                data.keys.forEach { key ->
                    val fileList = dav.WEBDAV_DATA.getOrDefault(key, mutableSetOf())
                    val dataFileList = data.getOrDefault(key, mutableSetOf())
                    fileList.removeAll(dataFileList)
                    fileList.addAll(dataFileList)
                    dav.WEBDAV_DATA[key] = fileList
                }
                dav.saveData()
                showToast("导入成功!")
                withContext(Dispatchers.Main) {
                    closeDialog()
                }
            }
            progress.LoadingContent()
        }
        setDefaultNegative()
        show()
    }
}