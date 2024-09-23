package com.donut.mixfile.util.file

import com.donut.mixfile.server.localClient
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.compressGzip
import com.donut.mixfile.util.decodeHex
import com.donut.mixfile.util.decompressGzip
import com.donut.mixfile.util.encodeToBase64
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.hashSHA256
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.toJsonString
import com.google.gson.Gson
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun exportFileList(fileList: List<FileDataLog>) {
    val strData = fileList.toJsonString()
    val compressedData = compressGzip(strData)
    doUploadFile(
        compressedData,
        "__mixfile_list_${compressedData.hashSHA256().decodeHex().encodeToBase64().take(8)}",
        false
    )
}

fun showFileList(fileList: List<FileDataLog>) {
    val fileTotalSize = fileList.sumOf { it.size }
    MixDialogBuilder(
        "文件列表",
        "共 ${fileList.size} 个文件 总大小: ${formatFileSize(fileTotalSize)}"
    ).apply {
        setContent {
            FileCardList(fileList)
        }
        setPositiveButton("导入文件") {
            val prevSize = favorites.size
            fileList.forEach {
                favCategories += it.category
            }
            favorites += fileList
            showToast("导入了 ${favorites.size - prevSize} 个文件")
            closeDialog()
        }
        show()
    }
}

suspend fun loadFileList(url: String, progressContent: ProgressContent): Array<FileDataLog>? {
    try {
        return localClient.prepareGet {
            timeout {
                requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
            }
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
            val data = it.bodyAsChannel().readRemaining(1024 * 1024 * 50).readBytes()
            val extractedData = decompressGzip(data)
            return@execute Gson().fromJson(extractedData, Array<FileDataLog>::class.java)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            showErrorDialog(e, "解析分享列表失败!")
        }
    }
    return null
}