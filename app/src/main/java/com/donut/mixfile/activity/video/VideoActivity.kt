package com.donut.mixfile.activity.video

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.core.net.toUri
import com.donut.mixfile.activity.video.player.VideoPlayerScreen
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.objects.MixActivity
import kotlinx.serialization.Serializable


var playHistory by cachedMutableOf(listOf<VideoHistory>(), "video_player_history_v2")

@Serializable
data class VideoHistory(val time: Long, val hash: String, val episode: Int)

class VideoActivity : MixActivity("video") {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoPlayerUrl = intent.getStringExtra("url") ?: ""
        val fileList = intent.getStringExtra("fileList") ?: ""
        var videos = listOf(videoPlayerUrl.toUri())
        val videoHash = intent.getStringExtra("hash") ?: videoPlayerUrl
        if (fileList.isNotEmpty()) {
            videos = fileList.split("\n").map { it.toUri() }
        }
        enterFullScreen()
        // 设置保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                if (videos.isEmpty()) {
                    Text(text = "视频url为空")
                    return@MainTheme
                }
                VideoPlayerScreen(
                    videoUris = videos,
                    hash = videoHash
                )
                return@MainTheme
            }
        }
    }

    private fun enterFullScreen() {
        val decorView = window.decorView
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}

