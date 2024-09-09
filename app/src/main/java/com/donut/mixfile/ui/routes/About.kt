package com.donut.mixfile.ui.routes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.app
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.formatFileSize

var uploadedDataBytes by cachedMutableOf(0, "uploaded_data_bytes")
var downloadedDataBytes by cachedMutableOf(0, "downloaded_data_bytes")

@Synchronized
fun increaseUploadData(size: Long) {
    uploadedDataBytes += size
}

@Synchronized
fun increaseDownloadData(size: Long) {
    downloadedDataBytes += size
}

@OptIn(ExperimentalLayoutApi::class)
val About = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    OutlinedCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = "统计",
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "已上传数据: ",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = formatFileSize(uploadedDataBytes),
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "已下载数据: ",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Text(
                    text = formatFileSize(downloadedDataBytes),
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            OutlinedButton(onClick = {
                MixDialogBuilder("确定重置统计?").apply {
                    setPositiveButton("确定") {
                        uploadedDataBytes = 0
                        downloadedDataBytes = 0
                        it()
                    }
                    show()
                }
            }) {
                Text(text = "重置")
            }
        }
    }
    Text(
        color = colorScheme.primary,
        text = "项目地址: https://gitlab.com/ivgeek/MixFile",
        modifier = Modifier.clickable {
            MixDialogBuilder("确定打开?").apply {
                setPositiveButton("确定") {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://gitlab.com/ivgeek/MixFile")
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    app.startActivity(intent)
                    closeDialog()
                }
                setDefaultNegative()
                show()
            }
        }
    )
    Text(
        color = Color.Gray,
        text = """
        MixFile采用混合式文件加密上传系统
        您上传的所有文件都会使用AES-GCM-128算法加密,
        上传时会生成随机的128位密钥在本地进行加密后进行上传,
        只要不泄漏分享码,文件内容是无法被任何人得知的
        分享码中包含了,文件地址，文件大小,加密使用的密钥等信息
        分享码默认使用不可见字符隐写到普通文本中进行编码信息
        如果长度超过发送限制可关闭使用短分享码功能
        请把应用省电改为无限制,否则文件服务器可能无法在后台运行
        开启自启动权限后,关闭主页面时将会自动在后台运行服务器
    """.trimIndent()
    )
}