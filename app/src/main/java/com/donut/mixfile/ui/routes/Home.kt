package com.donut.mixfile.ui.routes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.serverPort
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.file.FileDataLog
import com.donut.mixfile.util.file.InfoText
import com.donut.mixfile.util.file.deleteUploadLog
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.showFileShareDialog
import com.donut.mixfile.util.file.uploadLogs
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.formatTime
import com.donut.mixfile.util.getIpAddressInLocalNetwork
import com.donut.mixfile.util.readClipBoardText

var serverAddress by mutableStateOf("http://${getIpAddressInLocalNetwork()}:${serverPort}")

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
val Home = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally
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
        text = "局域网地址: ${serverAddress}",
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
                selectAndUploadFile()
            },
            modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp),
        ) {
            Text(text = "上传文件")
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileCard(fileDataLog: FileDataLog, showDate: Boolean = true, longClick: () -> Unit = {}) {
    HorizontalDivider()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(107, 218, 246, 0),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    longClick()
                }
            ) {
                tryResolveFile(fileDataLog.shareInfoData)
            }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = fileDataLog.name.trim(),
                color = colorScheme.primary,
                fontSize = 16.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoText(key = "大小: ", value = formatFileSize(fileDataLog.size))
                if (showDate) {
                    Text(
                        text = formatTime(fileDataLog.time),
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
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



