package com.donut.mixfile.server.routes

import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.encodeURL
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.objects.SortedTask
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.ranges
import io.ktor.server.response.contentRange
import io.ktor.server.response.header
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.io.encoding.ExperimentalEncodingApi

var DOWNLOAD_TASK_COUNT by cachedMutableOf(5, "download_task_count")

@OptIn(ExperimentalEncodingApi::class)
fun getDownloadRoute(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return route@{
        val shareInfoData = call.request.queryParameters["s"]
        val referer = call.request.queryParameters["referer"]
        if (shareInfoData == null) {
            call.respondText("分享信息为空", status = HttpStatusCode.InternalServerError)
            return@route
        }
        val shareInfo = resolveMixShareInfo(shareInfoData)
        if (shareInfo == null) {
            call.respondText("解析文件失败", status = HttpStatusCode.InternalServerError)
            return@route
        }
        val mixFile = shareInfo.fetchMixFile()
        if (mixFile == null) {
            call.respondText(
                "解析文件索引失败",
                status = HttpStatusCode.InternalServerError
            )
            return@route
        }
        var contentLength = shareInfo.fileSize
        val range: LongRange? = call.request.ranges()?.mergeToSingle(contentLength)
        call.response.apply {
            header(
                "Content-Disposition",
                "inline; filename=\"${shareInfo.fileName.encodeURL()}\""
            )
        }
        var fileList = mixFile.fileList.map { it to 0 }
        if (range != null) {
            fileList = mixFile.getFileListByRange(range)
            call.response.apply {
                header("Accept-Ranges", "bytes")
                status(HttpStatusCode.PartialContent)
                contentRange(range, mixFile.fileSize)
            }
            contentLength = range.last - range.first + 1
        }
        responseFileStream(call, fileList, contentLength, shareInfo, referer)
    }
}

private suspend fun responseFileStream(
    call: ApplicationCall,
    fileDataList: List<Pair<String, Int>>,
    contentLength: Long,
    shareInfo: MixShareInfo,
    referer: String?,
) {
    coroutineScope {
        val fileList = fileDataList.toMutableList()
        call.respondBytesWriter(
            contentType = ContentType.parse(shareInfo.contentType()).withCharset(Charsets.UTF_8),
            contentLength = contentLength
        ) {
            val sortedTask = SortedTask(DOWNLOAD_TASK_COUNT.toInt())
            val tasks = mutableListOf<Deferred<Unit>>()
            while (!isClosedForWrite && fileList.isNotEmpty()) {
                val currentMeta = fileList.removeAt(0)
                val taskOrder = -fileList.size
                sortedTask.prepareTask(taskOrder)
                tasks.add(async {
                    val url = currentMeta.first
                    val dataBytes = shareInfo.fetchFile(url, referer ?: shareInfo.referer)
                    val range = currentMeta.second
                    if (dataBytes == null) {
                        call.respondText(
                            "下载失败",
                            status = HttpStatusCode.InternalServerError
                        )
                        return@async
                    }
                    sortedTask.addTask(taskOrder) {
                        val dataToWrite = when {
                            range == 0 -> dataBytes
                            range < 0 -> dataBytes.copyOfRange(0, -range)
                            else -> dataBytes.copyOfRange(range, dataBytes.size)
                        }
                        try {
                            writeFully(dataToWrite)
                        } catch (e: Exception) {
                            close(e)
                        }
                    }
                    sortedTask.execute()
                })
            }
            tasks.awaitAll()
        }
    }
}