package com.donut.mixfile.util.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donut.mixfile.server.localClient
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.GSON
import com.donut.mixfile.util.compressGzip
import com.donut.mixfile.util.decompressGzip
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.getCurrentTime
import com.donut.mixfile.util.hashSHA256
import com.donut.mixfile.util.hashToMD5String
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.toHex
import com.donut.mixfile.util.toJsonString
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

fun exportFileList(fileList: Collection<FileDataLog>, name: String) {
    val strData = fileList.toJsonString()
    val compressedData = compressGzip(strData)
    doUploadFile(
        compressedData,
        "${name}.mix_list",
        false
    )
}

fun showExportFileListDialog(fileList: Collection<FileDataLog>) {
    MixDialogBuilder("确定导出?").apply {
        var listName by mutableStateOf("文件列表-${getCurrentTime()}")
        setContent {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedTextField(value = listName, onValueChange = {
                    listName = it
                }, modifier = Modifier.fillMaxWidth(), label = {
                    Text(text = "列表名称")
                })
                Text(text = "将会导出当前筛选的文件列表上传为一键分享链接")
            }
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            exportFileList(fileList, listName)
            closeDialog()
        }
        show()
    }
}

fun showFileList(fileList: List<FileDataLog>) {
    val fileTotalSize = fileList.sumOf { it.size }
    MixDialogBuilder(
        "文件列表",
        "共 ${fileList.size} 个文件 总大小: ${formatFileSize(fileTotalSize)}",
        tag = "file-list-${fileList.joinToString { it.shareInfoData }.hashSHA256().toHex()}"
    ).apply {
        setContent {
            FileCardList(fileList)
        }
        setPositiveButton("导入文件") {
            val prevSize = favorites.size
            val fileMap = favorites.map { it.shareInfoData }.toSet()
            fileList.forEach {
                favCategories += it.category
                if (!fileMap.contains(it.shareInfoData)) {
                    favorites += it
                }
            }
            showToast("导入了 ${favorites.size - prevSize} 个文件")
            closeDialog()
        }
        show()
    }
}

suspend fun loadFileList(url: String, progressContent: ProgressContent): Array<FileDataLog>? {
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
            val extractedData = decompressGzip(data)
            return@execute GSON.fromJson(extractedData, Array<FileDataLog>::class.java)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            showErrorDialog(e, "解析分享列表失败!")
        }
    }
    return null
}