package com.donut.mixfile.server.uploaders

import com.donut.mixfile.server.Uploader
import com.donut.mixfile.server.uploadClient
import com.donut.mixfile.util.cachedMutableOf
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var CUSTOM_UPLOAD_URL by cachedMutableOf("", "CUSTOM_UPLOAD_URL")

var CUSTOM_REFERER by cachedMutableOf("", "CUSTOM_REFERER")

object CustomUploader : Uploader("自定义") {

    override suspend fun genHead(): ByteArray {
        return uploadClient.get {
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

    override suspend fun doUpload(fileData: ByteArray): String {
        val response = uploadClient.put {
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