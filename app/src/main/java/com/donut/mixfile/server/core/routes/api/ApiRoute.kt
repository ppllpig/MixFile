package com.donut.mixfile.server.core.routes.api

import com.alibaba.fastjson2.toJSONString
import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.interceptCall
import com.donut.mixfile.server.core.routes.api.webdav.getWebDAVRoute
import com.donut.mixfile.server.core.utils.isNotNull
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun MixFileServer.getAPIRoute(): Route.() -> Unit {
    return {
        route("/webdav") {
            interceptCall({
                if (!webDav.loaded) {
                    it.respond(HttpStatusCode.ServiceUnavailable, "WebDav is Loading")
                }
            }, getWebDAVRoute())
        }
        get("/download/{name?}", getDownloadRoute())
        put("/upload/{name?}", getUploadRoute())
        get("/upload_history") {
            if (call.request.header("origin").isNotNull()) {
                return@get call.respondText("此接口禁止跨域", status = HttpStatusCode.Forbidden)
            }
            call.respond(getFileHistory())
        }
        get("/file_info") {
            val shareInfoStr = call.parameters["s"]
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
            call.respondText(object {
                val name = shareInfo.fileName
                val size = shareInfo.fileSize
            }.toJSONString())
        }
    }
}