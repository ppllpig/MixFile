package com.donut.mixfile.ui.routes.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.donut.mixfile.server.core.utils.isFalse
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.mixFileServer
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.UploadDialogCard
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.file.FileCardList
import com.donut.mixfile.util.file.deleteUploadLog
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.showFileInfoDialog
import com.donut.mixfile.util.file.showFileList
import com.donut.mixfile.util.file.toDataLog
import com.donut.mixfile.util.file.uploadLogs
import com.donut.mixfile.util.getIpAddressInLocalNetwork
import com.donut.mixfile.util.readClipBoardText
import com.donut.mixfile.util.showToast

var serverAddress by mutableStateOf("http://${getIpAddressInLocalNetwork()}:${mixFileServer.serverPort}")

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
        isError = text.isNotEmpty() && !tryResolveFileList(text.trim())
    }

    Row {
        OutlinedButton(
            onClick = {
                text = readClipBoardText()
                tryResolveFileList(text.trim())
            }, modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp)
        ) {
            Text(text = "粘贴内容")
        }
        Button(
            onClick = {
                tryResolveFileList(text.trim()).isFalse {
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
    UploadDialogCard()
    DownloadDialogCard()
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
            HorizontalDivider()
            FileCardList(cardList = uploadLogs.asReversed()) {
                deleteUploadLog(it)
            }
        }
    }

}

fun tryResolveFileList(text: String): Boolean {
    val textList = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val fileList = textList.mapNotNull { resolveMixShareInfo(it) }
    if (fileList.isEmpty()) {
        return false
    }
    if (fileList.size == 1) {
        showFileInfoDialog(fileList.first().toDataLog())
        return true
    }
    val fileDataList = fileList.map { it.toDataLog() }
    showFileList(fileDataList)
    return true
}


fun tryResolveFile(text: String): Boolean {
    val fileInfo = resolveMixShareInfo(text.trim())
    if (fileInfo != null) {
        showFileInfoDialog(fileInfo.toDataLog())
        return true
    }
    return false
}


fun getLocalServerAddress(): String {
    return "http://127.0.0.1:${mixFileServer.serverPort}"
}



