package com.donut.mixfile.ui.routes.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.serverPort
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.favorites.FileCard
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.file.cancelAllMultiUpload
import com.donut.mixfile.util.file.deleteUploadLog
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.showFileShareDialog
import com.donut.mixfile.util.file.uploadLogs
import com.donut.mixfile.util.file.uploadQueue
import com.donut.mixfile.util.getIpAddressInLocalNetwork
import com.donut.mixfile.util.isFalse
import com.donut.mixfile.util.readClipBoardText
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showToast

var serverAddress by mutableStateOf("http://${getIpAddressInLocalNetwork()}:${serverPort}")

fun showUploadTaskWindow() {
    MixDialogBuilder("上传中的文件").apply {
        setContent {
            if (uploadTasks.isEmpty()) {
                Text(text = "没有上传中的文件")
                return@setContent
            }
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uploadQueue > 0) {
                    Text(
                        text = "排队中: ${uploadQueue}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.padding(0.dp)
                ) {
                    uploadTasks.forEach {
                        TaskCard(uploadTask = it) {
                            it.delete()
                        }
                    }
                }
            }
        }
        setPositiveButton("清除失败任务") {
            uploadTasks = uploadTasks.filter { !it.stopped }
            showToast("清除成功")
        }
        setNegativeButton("全部取消") {
            showConfirmDialog("确定取消全部上传任务?") {
                cancelAllMultiUpload()
                showToast("取消成功")
            }
        }
        show()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
val Home = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally,
    floatingButton = {
        FloatingActionButton(onClick = {
            selectAndUploadFile()
        }, modifier = Modifier.padding(10.dp, 50.dp)) {
            Icon(Icons.Filled.Add, "Upload File")
        }
    }
) {
    var text by remember {
        mutableStateOf("")
    }

    var isError by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
        },
        modifier = Modifier.fillMaxWidth(), label = {
            Text(text = "请输入分享码")
        },
        maxLines = 3,
        isError = isError,
        supportingText = if (isError) {
            { Text(text = "无效分享码") }
        } else null,
        trailingIcon = {
            if (text.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close,
                    tint = colorScheme.primary,
                    contentDescription = "clear",

                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        text = ""
                    })
            }
        }
    )

    Text(
        text = "局域网地址: $serverAddress",
        color = colorScheme.primary,
        modifier = Modifier.clickable {
            serverAddress.copyToClipboard()
        })

    LaunchedEffect(key1 = text) {
        isError = text.isNotEmpty() && !tryResolveFile(text.trim())
    }

    Row {
        OutlinedButton(
            onClick = {
                text = readClipBoardText()
            }, modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp)
        ) {
            Text(text = "粘贴内容")
        }
        Button(
            onClick = {
                tryResolveFile(text.trim()).isFalse {
                    showToast("解析失败!")
                }
            },
            modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp),
        ) {
            Text(text = "解析文件")
        }
    }
    AnimatedVisibility(visible = uploadTasks.any { it.uploading }) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable {
                        showUploadTaskWindow()
                    }
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uploadTasks.filter { it.uploading }.size} 个文件正在上传中",
                    modifier = Modifier,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }
    }
    if (uploadLogs.isNotEmpty()) {
        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "上传历史",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(0.dp, 1000.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val logs = uploadLogs.reversed()
                    items(uploadLogs.size) { index ->
                        FileCard(logs[index]) {
                            deleteUploadLog(logs[index])
                        }
                    }
                }
            }
        }
    }

}


fun tryResolveFile(text: String): Boolean {
    val fileInfo = resolveMixShareInfo(text.trim())
    if (fileInfo != null) {
        showFileShareDialog(fileInfo)
        return true
    }
    return false
}


fun getLocalServerAddress(): String {
    return "http://127.0.0.1:${serverPort}"
}



