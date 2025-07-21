package com.donut.mixfile.ui.routes.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.donut.mixfile.app
import com.donut.mixfile.server.CustomUploader
import com.donut.mixfile.server.JavaScriptUploader
import com.donut.mixfile.server.MIXFILE_CHUNK_SIZE
import com.donut.mixfile.server.SERVER_PASSWORD
import com.donut.mixfile.server.UPLOADERS
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.currentUploader
import com.donut.mixfile.server.openCustomUploaderWindow
import com.donut.mixfile.server.openJavaScriptUploaderWindow
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.util.file.filePreview
import com.donut.mixfile.util.showToast

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
    AnimatedVisibility(
        this is CustomUploader,
        enter = slideInVertically(),
        exit = shrinkOut()
    ) {
        SettingButton(text = "自定义线路设置: ") {
            openCustomUploaderWindow()
        }
    }
    AnimatedVisibility(
        this.name.contentEquals(JavaScriptUploader.name),
        enter = slideInVertically(),
        exit = shrinkOut()
    ) {
        SettingButton(text = "JS自定义线路设置: ") {
            openJavaScriptUploaderWindow()
        }
    }
}

fun selectUploader() {
    MixDialogBuilder("上传线路").apply {
        setContent {
            SingleSelectItemList(
                items = UPLOADERS.map { it.name },
                getLabel = { it },
                currentOption = currentUploader
            ) { option ->
                currentUploader = option
                closeDialog()
            }
        }
        show()
    }
}

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
