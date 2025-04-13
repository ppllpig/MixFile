package com.donut.mixfile.ui.routes.favorites

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.component.common.SingleSelectItemList
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.compareByName
import com.donut.mixfile.util.file.favCategories
import com.donut.mixfile.util.file.favorites
import com.donut.mixfile.util.file.loadFileList
import com.donut.mixfile.util.file.showFileList
import com.donut.mixfile.util.objects.ProgressContent
import com.donut.mixfile.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun openCategorySelect(default: String = "", onSelect: (String) -> Unit) {
    MixDialogBuilder("收藏分类").apply {
        setContent {
            SingleSelectItemList(
                favCategories.toList().sortedWith { str1, str2 -> str1.compareByName(str2) },
                default
            ) {
                onSelect(it)
                closeDialog()
            }
        }
        setPositiveButton("添加分类") {
            createCategory()
        }
        if (favCategories.contains(default)) {
            setNegativeButton("编辑分类") {
                editCategory(default) {
                    closeDialog()
                    openCategorySelect(it, onSelect)
                }
            }
        }
        show()
    }
}

fun openSortSelect(default: String = "", onSelect: (String) -> Unit) {
    MixDialogBuilder("排序选择").apply {
        setContent {
            SingleSelectItemList(listOf("最新", "最旧", "最大", "最小", "名称"), default) {
                onSelect(it)
                closeDialog()
            }
        }
        show()
    }
}

fun editCategory(name: String, callback: (String) -> Unit = {}) {
    MixDialogBuilder("编辑分类").apply {
        var newName by mutableStateOf(name)

        setContent {
            OutlinedTextField(value = newName, onValueChange = {
                newName = it.substring(0, minOf(it.length, 20)).trim()
            }, modifier = Modifier.fillMaxWidth())
        }
        setNegativeButton("删除分类") {
            deleteCategory(name) {
                callback(name)
                closeDialog()
            }
        }
        setPositiveButton("确定") {
            if (newName.trim().isEmpty()) {
                showToast("分类名不能为空")
                return@setPositiveButton
            }
            favCategories -= name
            favCategories += newName
            currentCategory = newName
            showToast("修改分类名称成功")
            favorites = favorites.map {
                if (it.category.contentEquals(name)) {
                    it.copy(category = newName)
                } else {
                    it
                }
            }
            closeDialog()
            callback(newName)
        }
        show()
    }
}

fun deleteCategory(name: String, callback: (String) -> Unit = {}) {
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
            callback(name)
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
            if (name.trim().isEmpty()) {
                showToast("分类名不能为空")
                return@setPositiveButton
            }
            favCategories += name
            showToast("添加分类成功")
            closeDialog()
        }
        setDefaultNegative()
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