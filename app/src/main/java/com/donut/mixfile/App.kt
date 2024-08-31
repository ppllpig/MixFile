package com.donut.mixfile


import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import com.donut.mixfile.server.FileService
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.objects.MixActivity
import com.donut.mixfile.util.showError
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient


val appScope by lazy { MainScope() }

lateinit var kv: MMKV

lateinit var cacheKv: MMKV

private lateinit var innerApp: Application


val currentActivity: MixActivity
    get() {
        return MixActivity.firstActiveActivity()!!
    }

val app: Application
    get() = innerApp

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            showError(e)
            if (Looper.myLooper() == null) {
                return@setDefaultUncaughtExceptionHandler
            }

            MixDialogBuilder("发生错误").apply {
                setContent {
                    Column(
                        modifier = Modifier
                            .heightIn(0.dp, 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = e.message ?: "未知错误",
                            color = Color.Red,
                            fontSize = 20.sp
                        )
                        Text(text = e.stackTraceToString())
                    }
                }
                setPositiveButton("复制错误信息") {
                    e.stackTraceToString().copyToClipboard()
                }
                setNegativeButton("关闭") {
                    closeDialog()
                }
                show()
            }
        }
        innerApp = this
        MMKV.initialize(this)
        kv = MMKV.defaultMMKV()
        cacheKv = MMKV.mmkvWithID("cache", app.cacheDir.absolutePath)
        cacheKv.enableAutoKeyExpire(MMKV.ExpireInMinute)
        appScope.launch {
            while (true) {
                cacheKv.allNonExpireKeys()
                delay(1000 * 60)
            }
        }
        startService(Intent(this, FileService::class.java))
    }

    override fun newImageLoader(): ImageLoader {
        return genImageLoader(this)
    }


}

fun genImageLoader(
    context: Context,
    initializer: () -> OkHttpClient = { OkHttpClient() },
    sourceListener: (ByteArray) -> Unit = {},
): ImageLoader {
    return ImageLoader.Builder(context).components {
        add { result, _, _ ->
            val source = result.source.source()
            sourceListener(source.peek().readByteArray())
            null
        }
        if (Build.VERSION.SDK_INT >= 28) {
            add(ImageDecoderDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
        add(SvgDecoder.Factory())
        add(VideoFrameDecoder.Factory())
    }.okHttpClient(initializer)
        .crossfade(true).build()
}