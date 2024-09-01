package com.donut.mixfile.server.routes

import com.donut.mixfile.app
import com.donut.mixfile.server.utils.concurrencyLimit
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.file.uploadLogs
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.header
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
            val file = call.request.path().substring(1).ifEmpty {
                "index.html"
            }
            call.response.apply {
                header("Content-Type", ContentType.defaultForFilePath(file).contentType.run {
                    val extension = file.substringAfterLast(".")
                    when (extension) {
                        "js" -> "text/javascript"
                        "html" -> "text/html"
                        "css" -> "text/css"
                        else -> this
                    }
                })
            }

            try {
                val fileStream = app.assets.open(file)
                call.respondBytesWriter {
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
                val fileList = Gson().toJson(uploadLogs)
                call.respond(fileList)
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

