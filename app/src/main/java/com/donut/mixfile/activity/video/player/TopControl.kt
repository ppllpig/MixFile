package com.donut.mixfile.activity.video.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.currentActivity
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.formatTime
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopControl(title: String, visible: Boolean, modifier: Modifier) {
    AnimatedVisibility(
        enter = slideInVertically(
            initialOffsetY = { -it } // 从顶部（负方向）滑入
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it } // 向上（负方向）滑出
        ),
        visible = visible,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(10.dp, 15.dp),
        ) {
            Row(
                modifier = Modifier.weight(0.8f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.width(40.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = {
                            currentActivity.finish()
                        },
                    ) {
                        Icon(
                            modifier = Modifier.size(100.dp),
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Exit",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val context = LocalContext.current
            val batteryManager =
                context.getSystemService(BATTERY_SERVICE) as BatteryManager

            var isCharging by remember { mutableStateOf(batteryManager.isCharging) }
            val batteryLevel: Int =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val batteryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                }
            }

            // 使用 DisposableEffect 管理广播接收器的生命周期
            DisposableEffect(Unit) {
                val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                context.registerReceiver(batteryReceiver, intentFilter)

                // 在 Composable 销毁时取消注册
                onDispose {
                    context.unregisterReceiver(batteryReceiver)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    fontSize = 12.sp,
                    text = "电量: ${batteryLevel}%",
                    color = if (isCharging) Color.Green.copy(0.6f) else colorScheme.primary,
                )
                Text(
                    fontSize = 12.sp,
                    text = "时间: ${formatTime(Date(), "HH:mm")}",
                    color = colorScheme.primary,
                )
            }
        }
    }
}