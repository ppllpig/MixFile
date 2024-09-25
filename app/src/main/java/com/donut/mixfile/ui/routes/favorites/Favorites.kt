package com.donut.mixfile.ui.routes.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.routes.UploadDialogCard
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.file.FileCardList
import com.donut.mixfile.util.file.exportFileList
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.selectAndUploadFile
import com.donut.mixfile.util.file.updateMark
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.truncate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var currentCategory: String by mutableStateOf("")

private var favoriteSort by cachedMutableOf("最新", "mix_favorite_sort")


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

    OutlinedTextField(
        value = searchVal,
        onValueChange = {
            searchVal = it
        },
        label = { Text(text = "搜索") },
        modifier = Modifier.fillMaxWidth(),
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

    val scope = rememberCoroutineScope()

    LaunchedEffect(searchVal, currentCategory, favorites, updateMark, favoriteSort) {
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
        when (favoriteSort) {
            "最新" -> result = result.sortedByDescending { it.time }
            "最旧" -> result = result.sortedBy { it.time }
            "最大" -> result = result.sortedByDescending { it.size }
            "最小" -> result = result.sortedBy { it.size }
            "名称" -> {
                val resultCache = result
                scope.launch(Dispatchers.IO) {
                    val sorted = result.sortedBy { it.getNameNum() }
                    withContext(Dispatchers.Main) {
                        if (resultCache == result){
                            result = sorted
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
    UploadDialogCard()
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
        FileCardList(cardList = result)
    }
}