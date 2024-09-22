package com.donut.mixfile.server.routes

import com.donut.mixfile.server.Uploader
import com.donut.mixfile.server.getCurrentUploader
import com.donut.mixfile.server.utils.bean.MixFile
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.routes.home.UploadTask
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.generateRandomByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.math.ceil

var UPLOAD_TASK_COUNT by cachedMutableOf(10, "upload_task_count")


fun getUploadRoute(): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    return route@{
        val key = generateRandomByteArray(16)
        val name = call.request.queryParameters["name"]
        val add = call.request.queryParameters["add"] ?: "true"
        if (name.isNullOrEmpty()) {
            call.respondText("需要文件名称", status = HttpStatusCode.InternalServerError)
            return@route
        }
        val size = call.request.contentLength() ?: 0
        if (size <= 0L) {
            call.respondText("文件大小不合法", status = HttpStatusCode.InternalServerError)
            return@route
        }
        val uploadTask = UploadTask(call, name, size, add = add.toBoolean())
        currentCoroutineContext().job.invokeOnCompletion {
            uploadTask.error = it
            uploadTask.stopped = true
        }
        val uploader = getCurrentUploader()
        val head = uploader.genHead()
        val mixUrl =
            uploadFile(call.receiveChannel(), head, uploader, key, fileSize = size, uploadTask)
        if (mixUrl == null) {
            call.respondText("上传失败", status = HttpStatusCode.InternalServerError)
            return@route
        }
        val mixShareInfo =
            MixShareInfo(
                fileName = name,
                fileSize = size,
                headSize = head.size,
                url = mixUrl,
                key = MixShareInfo.ENCODER.encode(key),
                referer = uploader.referer
            )
        call.respondText(mixShareInfo.toString())
        uploadTask.complete(mixShareInfo)
    }
}

val semaphore = Semaphore(UPLOAD_TASK_COUNT.toInt())

suspend fun uploadFile(
    channel: ByteReadChannel,
    head: ByteArray,
    uploader: Uploader,
    secret: ByteArray,
    fileSize: Long,
    uploadTask: UploadTask,
): String? {
    return coroutineScope {
        val chunkSize = uploader.chunkSize
        //固定大小string list
        val fileListLength = ceil(fileSize.toDouble() / chunkSize).toInt()
        val fileList = List(fileListLength) { "" }.toMutableList()
        var fileIndex = 0
        val tasks = mutableListOf<Deferred<Unit?>>()

        while (!channel.isClosedForRead) {
            semaphore.acquire()
            val fileData = channel.readRemaining(chunkSize).readBytes()
            val currentIndex = fileIndex
            fileIndex++
            tasks.add(async {
                try {
                    val url = uploader.upload(head, fileData, secret) ?: return@async null
                    fileList[currentIndex] = url
                    withContext(Dispatchers.Main) {
                        uploadTask.progress.updateProgress(channel.totalBytesRead, fileSize)
                    }
                } finally {
                    semaphore.release()
                }
            })
        }
        tasks.awaitAll()
        if (fileList.any { it.isEmpty() }) {
            return@coroutineScope null
        }
        val mixFile =
            MixFile(chunkSize = chunkSize, version = 0, fileList = fileList, fileSize = fileSize)
        val mixFileUrl =
            uploader.upload(head, mixFile.toBytes(), secret)
                ?: return@coroutineScope null
        return@coroutineScope mixFileUrl
    }
}