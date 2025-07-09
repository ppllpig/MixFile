package com.donut.mixfile.server

import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.uploaders.A3Uploader
import com.donut.mixfile.server.core.uploaders.A1Uploader
import com.donut.mixfile.server.core.uploaders.A2Uploader
import com.donut.mixfile.util.cachedMutableOf
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var CUSTOM_UPLOAD_URL by cachedMutableOf("", "CUSTOM_UPLOAD_URL")

var CUSTOM_REFERER by cachedMutableOf("", "CUSTOM_REFERER")

val UPLOADERS = listOf(A1Uploader, A2Uploader, A3Uploader, CustomUploader)

var currentUploader by cachedMutableOf(A1Uploader.name, "current_uploader")

fun getCurrentUploader() = UPLOADERS.firstOrNull { it.name == currentUploader } ?: A1Uploader

object CustomUploader : Uploader("自定义") {

    override suspend fun genHead(client: HttpClient): ByteArray {
        return client.get {
            url(CUSTOM_UPLOAD_URL)
        }.also {
            val referer = it.headers["referer"]
            if (!referer.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    CUSTOM_REFERER = referer
                }
            }
        }.readRawBytes()
    }

    override val referer: String
        get() = CUSTOM_REFERER

    override suspend fun doUpload(fileData: ByteArray, client: HttpClient): String {
        val response = client.put {
            url(CUSTOM_UPLOAD_URL)
            setBody(fileData)
        }
        val resText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception(resText)
        }
        return resText
    }

}