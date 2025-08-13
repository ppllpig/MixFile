package com.donut.mixfile.ui.routes.webdav

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.UploadDialogCard
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.copyToClipboard
import com.donut.mixfile.util.readClipBoardText

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
val WebDAV = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    var text by remember {
        mutableStateOf("")
    }
    val webDavAddress = "$serverAddress/api/webdav"
    Text(
        text = "WebDAV局域网连接地址: $webDavAddress",
        color = colorScheme.primary,
        modifier = Modifier.clickable {
            webDavAddress.copyToClipboard()
        })
    UploadDialogCard()
    Button(onClick = {
        exportWebDavData()
    }, modifier = Modifier.fillMaxWidth()) {
        Text("导出文件")
    }
    OutlinedButton(onClick = {
        clearWebDavData()
    }, modifier = Modifier.fillMaxWidth()) {
        Text("清空文件")
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

    Text("支持webdav存档文件和mix_list文件导入", color = colorScheme.primary)

    Row {
        OutlinedButton(
            onClick = { text = readClipBoardText() },
            modifier = Modifier.weight(1f)
        ) {
            Text("粘贴内容")
        }
        Button(
            onClick = { tryImportWebDavData(text) },
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        ) {
            Text("导入文件")
        }
    }

    var downloadUrl by remember { mutableStateOf("") }
    OutlinedTextField(
        value = downloadUrl,
        onValueChange = { downloadUrl = it },
        label = { Text("请输入下载链接") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            if (downloadUrl.isNotEmpty()) {
                downloadToWebDav(downloadUrl)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("下载到 WebDAV")
    }

}