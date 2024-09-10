package com.donut.mixfile.ui.routes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.donut.mixfile.currentActivity
import com.donut.mixfile.server.UPLOADERS
import com.donut.mixfile.server.UPLOAD_RETRY_TIMES
import com.donut.mixfile.server.currentUploader
import com.donut.mixfile.server.getCurrentUploader
import com.donut.mixfile.server.routes.DOWNLOAD_TASK_COUNT
import com.donut.mixfile.server.routes.UPLOAD_TASK_COUNT
import com.donut.mixfile.server.uploaders.CUSTOM_REFERER
import com.donut.mixfile.server.uploaders.CUSTOM_UPLOAD_URL
import com.donut.mixfile.server.uploaders.CustomUploader
import com.donut.mixfile.server.uploaders.hidden.A1Uploader
import com.donut.mixfile.ui.component.common.CommonSwitch
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.util.TipText
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.file.uploadLogs
import com.donut.mixfile.util.showToast


var useShortCode by cachedMutableOf(true, "use_short_code")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingButton(text: String, buttonText: String = "设置", onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            OutlinedButton(onClick = onClick) {
                Text(text = buttonText)
            }
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context = currentActivity): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
fun openBatteryOptimizationSettings(context: Context = currentActivity) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
val MixSettings = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Column {
        Text(
            modifier = Modifier.padding(10.dp, 0.dp),
            text = "下载并发: ${DOWNLOAD_TASK_COUNT}",
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = DOWNLOAD_TASK_COUNT.toFloat() / 10f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                DOWNLOAD_TASK_COUNT = (it * 10).toLong().coerceAtLeast(1)
            }
        )

    }
    Column {
        Text(
            modifier = Modifier.padding(10.dp, 0.dp),
            text = "上传并发: ${UPLOAD_TASK_COUNT}",
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = UPLOAD_TASK_COUNT.toFloat() / 10f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                UPLOAD_TASK_COUNT = (it * 10).toLong().coerceAtLeast(1)
            }
        )
    }
    Column {
        Text(
            modifier = Modifier.padding(10.dp, 0.dp),
            text = "上传失败重试次数(单个分片): ${UPLOAD_RETRY_TIMES}",
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = UPLOAD_RETRY_TIMES.toFloat() / 20f,
            steps = 20,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                UPLOAD_RETRY_TIMES = (it * 20).toLong().coerceAtLeast(0)
            }
        )
    }
    TipText(
        content = """
        部分设置可能需要重启后才能生效
    """.trimIndent()
    )
    CommonSwitch(checked = useShortCode, text = "使用短分享码(空白字符编码信息):") {
        useShortCode = it
    }
    HorizontalDivider()
    ElevatedButton(onClick = {
        MixDialogBuilder("确定清除记录?").apply {
            setContent {
                Text(text = "确定清除所有上传历史记录?")
            }
            setDefaultNegative()
            setPositiveButton("确定") {
                closeDialog()
                uploadLogs = listOf()
                showToast("清除成功!")
            }
            show()
        }
    }, modifier = Modifier.fillMaxWidth()) {
        Text(text = "清除上传记录")
    }
    val batteryOptimization by remember {
        mutableStateOf(isIgnoringBatteryOptimizations())
    }
    if (!batteryOptimization) {
        ElevatedButton(onClick = {
            openBatteryOptimizationSettings()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "省电限制未设置!")
        }
    }
    SettingButton(text = "上传线路: $currentUploader") {
        selectUploader()
    }
    if (getCurrentUploader() == CustomUploader) {
        OutlinedTextField(value = CUSTOM_UPLOAD_URL, onValueChange = {
            CUSTOM_UPLOAD_URL = it
        }, label = { Text(text = "请求地址") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = CUSTOM_REFERER, onValueChange = {
            CUSTOM_REFERER = it
        }, label = { Text(text = "referer") }, modifier = Modifier.fillMaxWidth())
        Text(
            color = Color.Gray,
            text = """
        自定义线路请自行实现,app会使用put方式发送请求
        请求体为图片二进制,成功请返回200状态码,内容直接返回url
        失败返回403或500(会重试)状态码
        另外需要实现get方法返回填充图片,推荐gif格式
        图片尺寸不宜过大,否则影响上传速度
    """.trimIndent()
        )
    }
}

fun selectUploader() {
    MixDialogBuilder("上传线路").apply {
        setContent {
            SingleSelectItemList(
                items = UPLOADERS,
                getLabel = { it.name },
                currentOption = UPLOADERS.firstOrNull {
                    it.name == currentUploader
                } ?: A1Uploader
            ) { option ->
                currentUploader = option.name
                closeDialog()
            }
        }
        show()
    }
}

