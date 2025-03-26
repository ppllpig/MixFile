package com.donut.mixfile.activity.video.player

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@SuppressLint("PrivateResource")
@Composable
fun CenterControl(
    visible: Boolean,
    modifier: Modifier,
    player: ExoPlayer,
    onPause: () -> Unit = {},
    onMediaChange: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }


    // 添加播放器状态监听
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering =
                    listOf(Player.STATE_BUFFERING, Player.STATE_IDLE).contains(playbackState)
            }


            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                onMediaChange()
                super.onMediaItemTransition(mediaItem, reason)
            }


            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady
            }
        }
        player.addListener(listener)

        // 清理资源
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    AnimatedVisibility(isBuffering, modifier = modifier) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(40.dp)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(60.dp)) {
            IconButton(
                modifier = Modifier.scale(1.5f),
                onClick = {
                    player.seekBack()
                },
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            IconButton(
                modifier = Modifier.scale(1.5f),
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                    onPause()
                },
            ) {
                AnimatedVisibility(!isBuffering) {
                    Icon(
                        modifier = Modifier.size(100.dp),
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }

            }
            IconButton(
                modifier = Modifier.scale(1.5f),
                onClick = {
                    player.seekForward()
                },
            ) {
                Icon(
                    modifier = Modifier.size(100.dp),
                    imageVector = Icons.Default.KeyboardDoubleArrowRight,
                    contentDescription = "Forward",
                    tint = Color.White
                )
            }
        }
    }
}
