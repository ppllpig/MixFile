package com.donut.mixfile.server.uploaders;

import com.donut.mixfile.server.Uploader
import com.donut.mixfile.server.uploadClient
import com.donut.mixfile.server.utils.fileFormHeaders
import com.donut.mixfile.util.add
import com.google.gson.JsonObject
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.quote

object A3Uploader : Uploader("线路A3") {

    override val referer: String
        get() = ""

    override suspend fun doUpload(fileData: ByteArray): String? {
        val result = uploadClient.submitFormWithBinaryData("https://pic.2xb.cn/uppic.php?type=qq",
            formData {
                add("file", fileData, fileFormHeaders())
            }).body<JsonObject>()
        val code = result.get("code").asInt
        if (code != 200) {
            return null
        }

        return result.get("url").asString
    }
}
