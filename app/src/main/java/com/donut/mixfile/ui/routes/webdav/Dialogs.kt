package com.donut.mixfile.ui.routes.webdav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.donut.mixfile.kv
import com.donut.mixfile.server.WEB_DAV_KEY
import com.donut.mixfile.server.core.routes.api.webdav.objects.WebDavFile
import com.donut.mixfile.server.core.routes.api.webdav.objects.WebDavManager
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.mixFileServer
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.AsyncEffect
import com.donut.mixfile.util.errorDialog
import com.donut.mixfile.util.file.doUploadFile
import com.donut.mixfile.util.file.downloadUrl
import com.donut.mixfile.util.file.loadDataWithMaxSize
import com.donut.mixfile.util.file.loadFileList
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getCurrentTime
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            mixFileServer.webDav.WEBDAV_DATA.files.clear()
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


fun importFileListToWebDav(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("解析中").apply {
        setContent {
            AsyncEffect {
                errorDialog("解析文件失败", onError = { closeDialog() }) {
                    val dav = mixFileServer.webDav
                    val fileList = loadFileList(url, progress)
                    dav.importMixList(fileList)
                    dav.saveData()
                    showToast("导入完成")
                    withContext(Dispatchers.Main) {
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

suspend fun importWebDavData(data: WebDavFile) {
    val dav = mixFileServer.webDav
    data.files.forEach {
        dav.WEBDAV_DATA.addFile(it.value)
    }
    dav.saveData()
}

fun importWebDavData(manager: WebDavManager) {
    MixDialogBuilder(
        "导入中",
        autoClose = false
    ).apply {
        setContent {
            AsyncEffect {
                importWebDavData(manager.WEBDAV_DATA)
                showToast("导入成功")
                closeDialog()
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        show()
    }
}

fun importWebDavData(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("导入中").apply {
        setContent {
            AsyncEffect {
                errorDialog("导入失败", onError = { closeDialog() }) {
                    val webDavData = loadDataWithMaxSize(url, progress)
                    val dav = mixFileServer.webDav
                    val data = dav.parseDataFromBytes(webDavData)
                    importWebDavData(data)
                    showToast("导入成功!")
                }
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