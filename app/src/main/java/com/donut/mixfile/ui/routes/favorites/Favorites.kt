package com.donut.mixfile.ui.routes.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
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
import com.donut.mixfile.server.localClient
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.home.tryResolveFile
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.compressGzip
import com.donut.mixfile.util.decodeHex
import com.donut.mixfile.util.decompressGzip
import com.donut.mixfile.util.encodeToBase64
import com.donut.mixfile.util.file.FileDataLog
import com.donut.mixfile.util.file.InfoText
import com.donut.mixfile.util.file.deleteFavoriteLog
import com.donut.mixfile.util.file.doUploadFile
import com.donut.mixfile.util.file.favCategories
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.updateMark
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.formatTime
import com.donut.mixfile.util.hashSHA256
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showErrorDialog
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.toJsonString
import com.donut.mixfile.util.truncate
import com.google.gson.Gson
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun exportFileList(fileList: List<FileDataLog>) {
    val strData = fileList.toJsonString()
    val compressedData = compressGzip(strData)
    doUploadFile(
        compressedData,
        "__mixfile_list_${compressedData.hashSHA256().decodeHex().encodeToBase64().take(8)}",
        false
    )
}

fun showFileList(fileList: List<FileDataLog>) {
    val fileTotalSize = fileList.sumOf { it.size }
    MixDialogBuilder(
        "文件列表",
        "共 ${fileList.size} 个文件 总大小: ${formatFileSize(fileTotalSize)}"
    ).apply {
        setContent {
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
                    items(fileList.size) { index ->
                        FileCard(fileList[index]) {

                        }
                    }
                }
            }
        }
        setPositiveButton("导入文件") {
            val prevSize = favorites.size
            fileList.forEach {
                favCategories += it.category
            }
            favorites += fileList
            showToast("导入了 ${favorites.size - prevSize} 个文件")
            closeDialog()
        }
        show()
    }
}

suspend fun loadFileList(url: String, progressContent: ProgressContent): Array<FileDataLog>? {
    try {
        return localClient.prepareGet {
            timeout {
                requestTimeoutMillis = 1000 * 60 * 60 * 24 * 30L
            }
            url(url)
            onDownload(progressContent.ktorListener)
        }.execute {
            if (!it.status.isSuccess()) {
                val text = if ((it.contentLength()
                        ?: (1024 * 1024)) < 1024 * 500
                ) it.bodyAsText() else "未知错误"
                throw Exception("下载失败: ${text}")
            }
            if ((it.contentLength() ?: 0) > 1024 * 1024 * 50) {
                throw Exception("文件过大")
            }
            val data = it.bodyAsChannel().readRemaining(1024 * 1024 * 50).readBytes()
            val extractedData = decompressGzip(data)
            return@execute Gson().fromJson(extractedData, Array<FileDataLog>::class.java)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            showErrorDialog(e, "解析分享列表失败!")
        }
    }
    return null
}

var currentCategory: String by mutableStateOf("")

val Favorites = MixNavPage(
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

    var searchVal by remember {
        mutableStateOf("")
    }

    var result by remember {
        mutableStateOf(favorites.reversed())
    }

    var sort by remember {
        mutableStateOf("最新")
    }

    if (favorites.isEmpty()) {
        Text(
            text = "暂未收藏文件",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary
        )
        return@MixNavPage
    }
    OutlinedTextField(value = searchVal, onValueChange = {
        searchVal = it
    }, label = { Text(text = "搜索") }, modifier = Modifier.fillMaxWidth())

    Text(
        text = "文件数量: ${result.size} 总大小: ${formatFileSize(result.sumOf { it.size })}",
        color = colorScheme.primary
    )

    LaunchedEffect(searchVal, currentCategory, favorites, updateMark, sort) {
        result = if (searchVal.trim().isNotEmpty()) {
            favorites.filter {
                it.name.contains(searchVal)
            }.reversed()
        } else {
            favorites.reversed()
        }
        result = result.filter {
            currentCategory.isEmpty() || it.category == currentCategory
        }
        when (sort) {
            "最新" -> result = result.sortedByDescending { it.time }
            "最旧" -> result = result.sortedBy { it.time }
            "最大" -> result = result.sortedByDescending { it.size }
            "最小" -> result = result.sortedBy { it.size }
        }
    }
    Row {
        OutlinedButton(
            onClick = {
                openCategorySelect(currentCategory) {
                    currentCategory = if (it.contentEquals(currentCategory)) {
                        ""
                    } else {
                        it
                    }
                }
            }, modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp)
        ) {
            Text(text = "分类: ${currentCategory.ifEmpty { "全部" }.truncate(3)}")
        }
        Button(
            onClick = {
                MixDialogBuilder("确定导出?").apply {
                    setContent {
                        Text(text = "将会导出当前筛选的文件列表上传为一键分享链接")
                    }
                    setDefaultNegative()
                    setPositiveButton("确定") {
                        exportFileList(result)
                        closeDialog()
                    }
                    show()
                }
            },
            modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp),
        ) {
            Text(text = "导出文件")
        }
    }
    if (result.isEmpty()) {
        Text(
            text = "没有搜索到文件",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary
        )
        return@MixNavPage
    }

    ElevatedCard(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "排序:${sort}",
            modifier = Modifier
                .clickable {
                    openSortSelect(sort) {
                        sort = it
                    }
                }
                .fillMaxWidth()
                .padding(10.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
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
                items(result.size) { index ->
                    FileCard(result[index]) {
                        deleteFavoriteLog(result[index])
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileCard(
    fileDataLog: FileDataLog,
    showDate: Boolean = true,
    longClick: () -> Unit = {},
) {
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