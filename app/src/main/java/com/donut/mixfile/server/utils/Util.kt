package com.donut.mixfile.server.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.donut.mixfile.util.genRandomString
import com.donut.mixfile.util.generateRandomByteArray
import com.donut.mixfile.util.ignoreError
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random


fun fileFormHeaders(
    suffix: String = ".gif",
    mimeType: String = "image/gif",
): Headers {
    return Headers.build {
        append(HttpHeaders.ContentType, mimeType)
        append(
            HttpHeaders.ContentDisposition,
            "filename=\"${genRandomString(5)}${suffix}\""
        )
    }
}

fun createBlankBitmap(
    width: Int = Random.nextInt(50, 100),
    height: Int = Random.nextInt(50, 100),
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255)))
    return bitmap
}

fun concurrencyLimit(
    limit: Int,
    route: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit,
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit {
    val tasks = CopyOnWriteArrayList<() -> Unit>()
    return route@{
        while (tasks.size > limit) {
            val remove = tasks.removeAt(0)
            ignoreError {
                remove()
            }
        }
        val cancel: () -> Unit = {
            launch {
                throw Throwable("服务器达到并发限制")
            }
        }
        tasks.add(cancel)
        route(Unit)
        tasks.remove(cancel)
    }
}

fun getRandomEncKey() = generateRandomByteArray(256)

fun Bitmap.compressToByteArray(
    useWebp: Boolean = true,
): ByteArray {
    val bitmap = this
    val stream = ByteArrayOutputStream()

    if (useWebp) {
        bitmap.compress(Bitmap.CompressFormat.WEBP, 0, stream)
    } else {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream)
    }

    return stream.toByteArray()
}

fun Bitmap.toGif(): ByteArray {
    val bos = ByteArrayOutputStream()
    val encoder = AnimatedGifEncoder()
    encoder.start(bos)
    encoder.addFrame(this)
    encoder.finish()
    return bos.toByteArray()
}
