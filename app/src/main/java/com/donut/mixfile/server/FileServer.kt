package com.donut.mixfile.server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donut.mixfile.appScope
import com.donut.mixfile.server.routes.getRoutes
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.genRandomString
import com.donut.mixfile.util.ignoreError
import com.donut.mixfile.util.showError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ServerSocket

var serverPort by mutableIntStateOf(4719)
val accessKey = genRandomString(32)
var enableAccessKey by cachedMutableOf(false, "enable_mix_file_access_key")
var serverStarted by mutableStateOf(false)

fun startServer() {
    appScope.launch(Dispatchers.IO) {
        serverPort = findAvailablePort(serverPort) ?: serverPort
        embeddedServer(CIO, port = serverPort, watchPaths = emptyList()) {
            intercept(ApplicationCallPipeline.Call) {
                val key = call.request.queryParameters["accessKey"]
                if (enableAccessKey && !key.contentEquals(accessKey)) {
                    call.respondText("网页端已被禁止访问,请到APP设置中开启")
                    finish()
                }
            }
            install(ContentNegotiation) {
                gson()
            }
            install(CORS) {
                allowOrigins { true }
                anyHost()
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Put)
                allowHeader(HttpHeaders.AccessControlAllowOrigin)
                allowHeader(HttpHeaders.AccessControlAllowMethods)
                allowHeader(HttpHeaders.ContentType)
                anyHost()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText(
                        "发生错误: ${cause.message} ${cause.stackTraceToString()}",
                        status = HttpStatusCode.InternalServerError
                    )
                    if (cause is IOException) {
                        return@exception
                    }
                    when (cause.message) {
                        "服务器达到并发限制" -> Unit
                        else -> showError(cause)
                    }
                }
            }
            routing(getRoutes())
        }.start(wait = false)
        delay(1000)
        serverStarted = true
    }
}

fun findAvailablePort(startPort: Int = 9527, endPort: Int = 65535): Int? {
    for (port in startPort..endPort) {
        ignoreError {
            // 尝试绑定到指定端口
            ServerSocket(port).use { serverSocket ->
                // 成功绑定，返回该端口
                return serverSocket.localPort
            }
        }
    }
    return null
}