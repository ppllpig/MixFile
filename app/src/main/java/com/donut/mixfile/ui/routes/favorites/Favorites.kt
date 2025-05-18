package com.donut.mixfile.ui.routes.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.UploadDialogCard
import com.donut.mixfile.ui.routes.home.DownloadDialogCard
import com.donut.mixfile.ui.routes.home.showDownloadTaskWindow
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.OnDispose
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.catchError
import com.donut.mixfile.util.compareByName
import com.donut.mixfile.util.file.FileCardList
import com.donut.mixfile.util.file.downloadFile
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.showExportFileListDialog
import com.donut.mixfile.util.file.showFileInfoDialog
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.showConfirmDialog
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

var currentCategory: String by mutableStateOf("")

private var favoriteSort by cachedMutableOf("最新", "mix_favorite_sort")
var result by mutableStateOf(listOf<FileDataLog>())

@OptIn(ExperimentalFoundationApi::class)
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

    OutlinedTextField(
        value = searchVal,
        onValueChange = {
            searchVal = it
        },
        label = { Text(text = "搜索") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2,
        trailingIcon = {
            if (searchVal.isNotEmpty()) {
                Icon(
                    Icons.Outlined.Close,
                    tint = colorScheme.primary,
                    contentDescription = "clear",

                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        searchVal = ""
                    })
            }
        })

    Text(
        text = "文件数量: ${result.size} 总大小: ${formatFileSize(result.sumOf { it.size })}",
        color = colorScheme.primary
    )

    OnDispose {
        result = listOf()
    }

    LaunchedEffect(searchVal, currentCategory, favorites, favoriteSort) {
        result = if (searchVal.trim().isNotEmpty()) {
            favorites.filter {
                it.name.contains(searchVal)
            }.asReversed()
        } else {
            favorites.asReversed()
        }
        result = result.filter {
            currentCategory.isEmpty() || it.category == currentCategory
        }
        when (favoriteSort) {
            "最新" -> result = result.sortedByDescending { it.time }
            "最旧" -> result = result.sortedBy { it.time }
            "最大" -> result = result.sortedByDescending { it.size }
            "最小" -> result = result.sortedBy { it.size }
            "名称" -> {
                val resultCache = result
                withContext(Dispatchers.IO) {
                    catchError {
                        val sorted = result.sortedWith { file1, file2 ->
                            if (!isActive) {
                                throw Exception("排序取消")
                            }
                            file1.name.compareByName(file2.name)
                        }
                        withContext(Dispatchers.Main) {
                            if (resultCache == result) {
                                result = sorted
                            }
                        }
                    }
                }
            }
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
            Text(
                text = "分类: ${currentCategory.ifEmpty { "全部" }}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = {
                showExportFileListDialog(result)
            },
            modifier = Modifier
                .weight(1.0f)
                .padding(10.dp, 0.dp),
        ) {
            Text(text = "导出文件")
        }
    }
    UploadDialogCard()
    DownloadDialogCard()
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

    var multiSelect by remember {
        mutableStateOf(false)
    }

    var selected by remember {
        mutableStateOf(setOf<FileDataLog>())
    }

    LaunchedEffect(multiSelect) {
        if (!multiSelect) {
            selected = setOf()
        }
    }

    LaunchedEffect(selected) {
        if (selected.isEmpty()) {
            multiSelect = false
        }
    }

    AnimatedVisibility(visible = multiSelect) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .combinedClickable(onLongClick = {
                        selected = emptySet()
                    }) {
                        showFileListActionDialog(
                            listOf(
                                Pair("删除") {
                                    showConfirmDialog("确定删除?") {
                                        favorites = favorites - selected
                                        showToast("删除成功")
                                        selected = emptySet()
                                    }
                                },
                                Pair("全选") {
                                    selected += result
                                },
                                Pair("取消选择") {
                                    selected = emptySet()
                                },
                                Pair("导出文件") {
                                    showExportFileListDialog(selected)
                                    selected = emptySet()
                                },
                                Pair("全部下载") {
                                    selected.forEach {
                                        downloadFile(it)
                                    }
                                    showDownloadTaskWindow()
                                    selected = emptySet()
                                },
                                Pair("移动分类") {
                                    openCategorySelect(selected.first().category) { category ->
                                        favorites = favorites.map {
                                            if (selected.contains(it))
                                                it.copy(category = category)
                                            else
                                                it
                                        }
                                        selected = emptySet()
                                    }
                                },
                            ),
                        )
                    }
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已选择 ${selected.size} 个文件",
                    modifier = Modifier,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "selected",
                    tint = colorScheme.primary
                )
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "排序:${favoriteSort}",
            modifier = Modifier
                .clickable {
                    openSortSelect(favoriteSort) {
                        favoriteSort = it
                    }
                }
                .fillMaxWidth()
                .padding(10.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
        )
        HorizontalDivider()
        FileCardList(
            cardList = result,
            selected = selected,
            onClick = {
                if (!multiSelect) {
                    showFileInfoDialog(it)
                    return@FileCardList
                }
                if (!selected.contains(it)) {
                    selected += it
                    return@FileCardList
                }
                selected -= it
            }) {
            multiSelect = true
            selected += it
        }
    }
}

fun showFileListActionDialog(options: List<Pair<String, () -> Unit>>) {
    MixDialogBuilder("编辑文件").apply {
        setContent {
            SingleSelectItemList(
                items = options,
                getLabel = {
                    it.first
                },
            ) { option ->
                option.second()
                closeDialog()
            }
        }
        show()
    }
}