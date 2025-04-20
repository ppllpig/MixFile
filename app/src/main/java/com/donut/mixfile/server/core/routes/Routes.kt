package com.donut.mixfile.server.core.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.routes.api.getAPIRoute
import com.donut.mixfile.server.core.utils.parseFileMimeType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel

fun MixFileServer.getRoutes(): Routing.() -> Unit {

    return {
        get("{param...}") {
            val file = call.request.path().substring(1).ifEmpty {
                "index.html"
            }
            val fileStream =
                getStaticFile(file) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondBytesWriter(
                contentType = file.parseFileMimeType()
            ) {
                fileStream.toByteReadChannel().copyTo(this)
            }
        }
        route("/api", getAPIRoute())

    }
}

