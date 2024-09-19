package com.donut.mixfile.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import com.donut.mixfile.server.localClient
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.compressGzip
import com.donut.mixfile.util.decompressGzip
import com.donut.mixfile.util.file.FileDataLog
import com.donut.mixfile.util.file.deleteFavoriteLog
import com.donut.mixfile.util.file.doUploadFile
import com.donut.mixfile.util.file.favCategories
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.ignoreError
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast
import com.donut.mixfile.util.toJsonString
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

fun openCategorySelect(default: String = "", onSelect: (String) -> Unit) {
    MixDialogBuilder("收藏分类").apply {
        setContent {
            SingleSelectItemList(favCategories.toList(), default) {
                onSelect(it)
                closeDialog()
            }
        }
        setPositiveButton("添加分类") {
            createCategory()
        }
        if (default.isNotEmpty()) {
            setNegativeButton("删除分类") {
                if (default.contentEquals("默认")) {
                    showToast("不能删除默认分类")
                }
                deleteCategory(default)
            }
        }
        show()
    }
}

fun deleteCategory(name: String) {
    MixDialogBuilder("确定删除分类?").apply {
        setContent {
            Text(text = "分类: ${name}")
            Text(text = "删除后将会移除此分类下所有文件!")
        }
        setDefaultNegative()
        setPositiveButton("确定") {
            favCategories -= name
            favorites = favorites.filter {
                it.category != name
            }
            showToast("删除分类成功")
            closeDialog()
        }
        show()
    }
}

fun createCategory() {
    MixDialogBuilder("新建分类").apply {
        var name by mutableStateOf("")
        setContent {
            OutlinedTextField(value = name, onValueChange = {
                name = it.substring(0, minOf(it.length, 20)).trim()
            }, modifier = Modifier.fillMaxWidth())
        }
        setPositiveButton("确认") {
            favCategories += name
            showToast("添加分类成功")
            closeDialog()
        }
        setDefaultNegative()
        show()
    }
}

fun exportFileList(fileList: List<FileDataLog>) {
    val strData = fileList.toJsonString()
    val compressedData = compressGzip(strData)
    doUploadFile(compressedData, "__mixfile_list")
}

fun showFileList(fileList: List<FileDataLog>) {
    MixDialogBuilder("文件列表").apply {
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
            favorites = favorites.distinct()
            showToast("导入了 ${favorites.size - prevSize} 个文件")
            closeDialog()
        }
        show()
    }
}

fun importFileList(url: String) {
    val progress = ProgressContent()
    MixDialogBuilder("解析中").apply {
        setContent {
            UseEffect {
                val fileList = loadFileList(url, progress)
                if (fileList == null) {
                    showToast("解析分享列表失败!")
                    closeDialog()
                    return@UseEffect
                }
                withContext(Dispatchers.Main) {
                    showFileList(fileList.toList())
                    closeDialog()
                }
            }
            progress.LoadingContent()
        }
        setDefaultNegative()
        show()
    }

}

suspend fun loadFileList(url: String, progressContent: ProgressContent): Array<FileDataLog>? {
    return ignoreError {
        localClient.prepareGet {
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
    }
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


    LaunchedEffect(key1 = searchVal, currentCategory, favorites) {
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
            Text(text = "筛选分类: ${currentCategory.ifEmpty { "全部" }}")
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
            text = "收藏的文件",
            modifier = Modifier
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