package com.donut.mixfile.activity

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.objects.MixActivity
import com.donut.mixfile.util.showToast
import io.sanghun.compose.video.RepeatMode
import io.sanghun.compose.video.VideoPlayer
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import kotlinx.parcelize.Parcelize


class VideoActivity : MixActivity("video") {

    private var playerInstance: ExoPlayer? = null
    private var playHistory by cachedMutableOf(listOf<VideoHistory>(), "video_player_history")

    @Parcelize
    data class VideoHistory(val time: Long, val url: String) : Parcelable {
        override fun equals(other: Any?): Boolean {
            return other is VideoHistory && other.url.contentEquals(url)
        }

        override fun hashCode(): Int {
            return url.hashCode()
        }
    }

    override fun onResume() {
        super.onResume()
        playerInstance?.play()
    }

    override fun onPause() {
        super.onPause()
        playerInstance?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerInstance?.release()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoPlayerUrl = intent.getStringExtra("url") ?: ""
        enterFullScreen()
        // 设置保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        var seeked = false
        setContent {
            MainTheme {
                if (videoPlayerUrl.trim().isEmpty()) {
                    Text(text = "视频url为空")
                    return@MainTheme
                }
                VideoPlayer(
                    mediaItems = listOf(
                        VideoPlayerMediaItem.NetworkMediaItem(videoPlayerUrl),
                    ),
                    controllerConfig = VideoPlayerControllerConfig(
                        showSpeedAndPitchOverlay = true,
                        showSubtitleButton = false,
                        showCurrentTimeAndTotalTime = true,
                        showBufferingProgress = true,
                        showForwardIncrementButton = true,
                        showBackwardIncrementButton = true,
                        showBackTrackButton = false,
                        showNextTrackButton = false,
                        showRepeatModeButton = false,
                        controllerShowTimeMilliSeconds = 5_000,
                        controllerAutoShow = false,
                        showFullScreenButton = false,
                    ),
                    playerInstance = {
                        playerInstance = this
                    },
                    onCurrentTimeChanged = { time ->
                        if (!seeked) {
                            val cached = playHistory.firstOrNull { it.url == videoPlayerUrl }
                            if (cached != null) {
                                playerInstance?.seekTo((cached.time - 2000L).coerceAtLeast(0))
                                showToast("已跳转到上次播放位置", length = Toast.LENGTH_SHORT)
                            }
                            seeked = true
                        } else {
                            playHistory.toMutableList().apply {
                                add(0, VideoHistory(time, videoPlayerUrl))
                                if (this.size > 10) {
                                    this.removeAt(this.lastIndex)
                                }
                                playHistory = this.distinct()
                            }
                        }
                    },

                    handleLifecycle = false,
                    autoPlay = true,
                    usePlayerController = true,
                    enablePip = false,
                    handleAudioFocus = true,
                    repeatMode = RepeatMode.ALL,
                    modifier = Modifier
                        .fillMaxSize()
                )
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

