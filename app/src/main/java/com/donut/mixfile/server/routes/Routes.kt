package com.donut.mixfile.server.routes

import com.donut.mixfile.app
import com.donut.mixfile.server.utils.concurrencyLimit
import com.donut.mixfile.util.debug
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.parseFileMimeType
import com.donut.mixfile.util.toJsonString
import com.google.gson.JsonObject
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.jvm.javaio.toOutputStream
import java.io.FileNotFoundException

fun getRoutes(): Routing.() -> Unit {

    return {

        get("{param...}") {
            debug("牛逼")
            val file = call.request.path().substring(1).ifEmpty {
                "index.html"
            }
            try {
                val fileStream = app.assets.open(file)
                call.respondBytesWriter(
                    contentType = ContentType.parse(file.parseFileMimeType())
                ) {
                    fileStream.copyTo(this.toOutputStream())
                }
            } catch (e: FileNotFoundException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        route("/api") {
            get("/download", concurrencyLimit(DOWNLOAD_TASK_COUNT.toInt(), getDownloadRoute()))
            put("/upload", concurrencyLimit(UPLOAD_TASK_COUNT.toInt(), getUploadRoute()))
            get("/upload_history") {
                call.respond(favorites.take(1000).toJsonString())
            }
            get("/file_info") {
                val shareInfoStr = call.request.queryParameters["s"]
                if (shareInfoStr == null) {
                    call.respondText("分享信息为空", status = HttpStatusCode.InternalServerError)
                    return@get
                }
                val shareInfo = resolveMixShareInfo(shareInfoStr)
                if (shareInfo == null) {
                    call.respondText(
                        "分享信息解析失败",
                        status = HttpStatusCode.InternalServerError
                    )
                    return@get
                }
                call.respondText(JsonObject().apply {
                    addProperty("name", shareInfo.fileName)
                    addProperty("size", shareInfo.fileSize)
                }.toString())
            }
        }
    }
}

