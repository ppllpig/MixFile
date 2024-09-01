package com.donut.mixfile.server

import com.donut.mixfile.server.uploaders.A3Uploader
import com.donut.mixfile.server.uploaders.hidden.A1Uploader
import com.donut.mixfile.server.uploaders.hidden.A2Uploader
import com.donut.mixfile.server.utils.createBlankBitmap
import com.donut.mixfile.server.utils.toGif
import com.donut.mixfile.ui.routes.increaseUploadData
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.encryptAES

val UPLOADERS = listOf(A1Uploader, A2Uploader, A3Uploader)

var currentUploader by cachedMutableOf(A1Uploader.name, "current_uploader")


fun getCurrentUploader() = UPLOADERS.firstOrNull { it.name == currentUploader } ?: A1Uploader

abstract class Uploader(val name: String) {

    open val referer = ""
    open val chunkSize = 1024L * 1024L

    abstract suspend fun doUpload(fileData: ByteArray): String?

    companion object {
        val urlTransforms = mutableMapOf<String, (String) -> String>()
        val refererTransforms = mutableMapOf<String, (url: String, referer: String) -> String>()

        fun transformUrl(url: String): String {
            return urlTransforms.entries.fold(url) { acc, (name, transform) ->
                transform(acc)
            }
        }

        fun transformReferer(url: String, referer: String): String {
            return refererTransforms.entries.fold(referer) { acc, (name, transform) ->
                transform(url, acc)
            }
        }

        fun registerUrlTransform(name: String, transform: (String) -> String) {
            urlTransforms[name] = transform
        }

        fun registerRefererTransform(
            name: String,
            transform: (url: String, referer: String) -> String,
        ) {
            refererTransforms[name] = transform
        }
    }

    suspend fun upload(head: ByteArray, fileData: ByteArray, key: ByteArray): String? {
        val encryptedData = encryptBytes(head, fileData, key)
        try {
            return doUpload(encryptedData)
        } finally {
            increaseUploadData(encryptedData.size.toLong())
        }
    }

    open fun genHead() = createBlankBitmap().toGif()
    private fun encryptBytes(head: ByteArray, fileData: ByteArray, key: ByteArray): ByteArray {
        return head + (encryptAES(fileData, key))
    }

}