package com.donut.mixfile.ui.routes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.donut.mixfile.app
import com.donut.mixfile.server.CUSTOM_REFERER
import com.donut.mixfile.server.CUSTOM_UPLOAD_URL
import com.donut.mixfile.server.CustomUploader
import com.donut.mixfile.server.DOWNLOAD_TASK_COUNT
import com.donut.mixfile.server.MIXFILE_CHUNK_SIZE
import com.donut.mixfile.server.SERVER_PASSWORD
import com.donut.mixfile.server.UPLOADERS
import com.donut.mixfile.server.UPLOAD_RETRY_COUNT
import com.donut.mixfile.server.UPLOAD_TASK_COUNT
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.uploaders.hidden.A1Uploader
import com.donut.mixfile.server.currentUploader
import com.donut.mixfile.server.getCurrentUploader
import com.donut.mixfile.ui.component.common.CommonSwitch
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.Theme
import com.donut.mixfile.ui.theme.currentTheme
import com.donut.mixfile.ui.theme.enableAutoDarkMode
import com.donut.mixfile.util.TipText
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.file.filePreview
import com.donut.mixfile.util.file.multiUploadTaskCount
import com.donut.mixfile.util.file.uploadLogs
import com.donut.mixfile.util.showToast


var useShortCode by cachedMutableOf(true, "use_short_code")
var autoAddFavorite by cachedMutableOf(true, "auto_add_favorite")
var useSystemPlayer by cachedMutableOf(false, "use_system_player")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingButton(
    text: String,
    buttonText: String = "设置",
    description: String = "",
    onClick: () -> Unit
) {
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
        if (description.isNotEmpty()) {
            Text(
                text = description,
                modifier = Modifier
                    .fillMaxWidth(),
//                    .padding(10.dp, 0.dp),
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp
            )
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context = app): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
fun openBatteryOptimizationSettings(context: Context = app) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
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
            text = "下载并发: $DOWNLOAD_TASK_COUNT",
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
            text = "上传并发: $UPLOAD_TASK_COUNT",
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
            text = "批量上传并发: ${multiUploadTaskCount}",
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = multiUploadTaskCount.toFloat() / 10f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                multiUploadTaskCount = (it * 10).toLong().coerceAtLeast(1)
            }
        )
    }
    Column {
        Text(
            modifier = Modifier.padding(10.dp, 0.dp),
            text = "上传失败重试次数(单个分片): $UPLOAD_RETRY_COUNT",
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = UPLOAD_RETRY_COUNT.toFloat() / 10f,
            steps = 10,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                UPLOAD_RETRY_COUNT = (it * 10).toLong().coerceAtLeast(0)
            }
        )
    }
    TipText(
        content = """
        部分设置可能需要重启后才能生效
    """.trimIndent()
    )
    CommonSwitch(
        checked = useShortCode,
        text = "使用短分享码(空白字符编码信息):"
    ) {
        useShortCode = it
    }
    CommonSwitch(
        checked = autoAddFavorite,
        text = "上传后自动添加文件到收藏:"
    ) {
        autoAddFavorite = it
    }
    CommonSwitch(
        checked = useSystemPlayer,
        text = "使用系统播放器:",
        description = "播放视频是否使用系统调用其他播放器"
    ) {
        useSystemPlayer = it
    }

    SettingButton(text = "网页端/WebDAV访问密码") {
        setWebPassword()
    }

    SettingButton(text = "文件分片大小", description = "默认1024kb,不建议修改") {
        setUploadChunkSize()
    }

    val uploader = remember(currentUploader) { getCurrentUploader() }

    SettingButton(text = "上传线路: ${uploader.name}") {
        selectUploader()
    }
    uploader.SettingComponent()
    SettingButton(text = "文件预览: $filePreview") {
        selectFilePreview()
    }
    SettingButton(text = "颜色主题: ") {
        MixDialogBuilder("颜色主题").apply {
            setContent {
                SingleSelectItemList(
                    items = Theme.entries,
                    getLabel = { it.label },
                    currentOption = Theme.entries.firstOrNull {
                        it.name == currentTheme
                    } ?: Theme.DEFAULT
                ) { option ->
                    currentTheme = option.name
                    closeDialog()
                }
            }
            show()
        }
    }
    CommonSwitch(
        checked = enableAutoDarkMode,
        text = "自动深色模式:",
        "跟随系统自动切换深色模式",
    ) {
        enableAutoDarkMode = it
    }
    HorizontalDivider()
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
    ElevatedButton(onClick = {
        MixDialogBuilder("确定清除记录?").apply {
            setContent {
                Text(text = "确定清除所有上传历史记录?")
            }
            setPositiveButton("确定") {
                closeDialog()
                uploadLogs = emptyList()
                showToast("清除成功!")
            }
            show()
        }
    }, modifier = Modifier.fillMaxWidth()) {
        Text(text = "清除上传记录")
    }
}

fun setUploadChunkSize() {
    MixDialogBuilder("设置分片大小").apply {
        var chunkSize by mutableLongStateOf(MIXFILE_CHUNK_SIZE)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = chunkSize.toString(),
                    onValueChange = {
                        chunkSize = it.toLongOrNull() ?: 0
                    },
                    maxLines = 1,
                    label = { Text(text = "文件分片大小(KB)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "有上传大文件(>20GB)需求可适量增大,会增加上传失败概率，增加上传下载时占用，降低下载速度,同时并发数量也会根据分片大小自动调整,基于1mb,如果并发为10,分片为2mb,实际并发数量则会为5",
                    modifier = Modifier
                        .fillMaxWidth(),
//                    .padding(10.dp, 0.dp),
                    color = Color(0xFF9E9E9E),
                    fontSize = 14.sp
                )
            }
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            MIXFILE_CHUNK_SIZE = chunkSize.coerceAtLeast(1).coerceAtMost(20480)
            showToast("设置成功")
            closeDialog()
        }
        show()
    }
}

fun setWebPassword() {
    MixDialogBuilder("设置密码").apply {
        var pass by mutableStateOf(SERVER_PASSWORD)
        setContent {
            OutlinedTextField(
                value = pass,
                onValueChange = {
                    pass = it
                },
                maxLines = 1,
                label = { Text(text = "网页/WEBDAV访问密码(留空禁用密码)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            SERVER_PASSWORD = pass.trim()
            showToast("设置成功")
            closeDialog()
        }
        show()
    }
}

fun selectFilePreview() {
    MixDialogBuilder("文件预览").apply {
        setContent {
            SingleSelectItemList(
                items = listOf("开启", "关闭", "仅Wifi"),
                currentOption = filePreview
            ) { option ->
                filePreview = option
                closeDialog()
            }
        }
        show()
    }
}

@Composable
fun Uploader.SettingComponent() {
    when (this) {
        is CustomUploader -> {
            OutlinedTextField(
                value = CUSTOM_UPLOAD_URL,
                onValueChange = {
                    CUSTOM_UPLOAD_URL = it.trim()
                },
                maxLines = 1,
                label = {
                    Text(text = "请求地址")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = CUSTOM_REFERER,
                maxLines = 1,
                onValueChange = {
                    CUSTOM_REFERER = it.trim()
                },
                label = {
                    Text(text = "referer")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                text = """
        自定义线路请自行实现,app会使用PUT方式发送请求
        请求体为图片二进制,成功请返回200状态码,内容直接返回URL
        失败返回403或500(会重试)状态码
        另外需要实现GET方法返回填充图片,推荐gif格式
        图片尺寸不宜过大,否则影响上传速度
        """.trimIndent(),
            )
        }
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

