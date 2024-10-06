package com.donut.mixfile.util.file

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.autoAddFavorite
import com.donut.mixfile.ui.routes.favorites.currentCategory
import com.donut.mixfile.util.TimestampAdapter
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.showToast
import com.google.gson.annotations.JsonAdapter
import java.util.Date


data class FileDataLog(
    var shareInfoData: String,
    var name: String,
    val size: Long,
    @JsonAdapter(TimestampAdapter::class)
    val time: Date = Date(),
    var category: String = currentCategory,
) {

    init {
        //限制category长度
        if (category.length > 20) {
            category = category.substring(0, 20)
        }
        category = category.trim()
    }

    fun rename() {
        val shareInfo = resolveMixShareInfo(shareInfoData) ?: return
        MixDialogBuilder("重命名文件").apply {
            var name by mutableStateOf(shareInfo.fileName)
            setContent {
                OutlinedTextField(value = name, onValueChange = {
                    name = it
                }, modifier = Modifier.fillMaxWidth(), label = {
                    Text(text = "输入文件名")
                })
            }
            setDefaultNegative()
            setPositiveButton("确定") {
                shareInfo.fileName = name
                this@FileDataLog.shareInfoData = shareInfo.toString()
                this@FileDataLog.name = name
                updateFavorites()
                showToast("重命名文件成功!")
                closeDialog()
            }
            show()
        }
    }

    override fun hashCode(): Int {
        return shareInfoData.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileDataLog) return false
        return shareInfoData.contentEquals(other.shareInfoData)
    }
}


var favorites by cachedMutableOf(listOf<FileDataLog>(), "favorite_file_logs")

var updateMark by mutableIntStateOf(0)
    private set

fun updateFavorites() {
    favorites = favorites
    updateMark++
}

var uploadLogs by cachedMutableOf(listOf<FileDataLog>(), "upload_file_logs")

var favCategories by cachedMutableOf(setOf("默认"), "fav_categories")

fun isFavorite(shareInfo: MixShareInfo): Boolean {
    return favorites.contains(shareInfo.toDataLog())
}

fun addUploadLog(shareInfo: MixShareInfo) {
    if (autoAddFavorite) {
        addFavoriteLog(shareInfo)
    }
    val uploadLog = shareInfo.toDataLog()
    if (uploadLogs.size > 1000) {
        uploadLogs = uploadLogs.drop(1)
    }
    uploadLogs = uploadLogs + uploadLog
}


fun deleteUploadLog(uploadLog: FileDataLog, callback: () -> Unit = {}) {
    MixDialogBuilder("确定删除?").apply {
        setContent {
            Text(text = "确定从上传记录中删除?")
        }
        setPositiveButton("确定") {
            uploadLogs = uploadLogs.filter { it != uploadLog }
            closeDialog()
            callback()
            showToast("删除成功")
        }
        setDefaultNegative()
        show()
    }
}


fun addFavoriteLog(
    shareInfo: MixShareInfo,
    category: String = currentCategory.ifEmpty { "默认" },
): Boolean {
    if (favorites.size > 10000) {
        showToast("收藏已达到限制!")
        return false
    }
    val favoriteLog = shareInfo.toDataLog()
    favoriteLog.category = category
    favCategories += category
    if (favorites.any { it == favoriteLog }) {
        favorites -= favoriteLog
        favorites += favorites
        return true
    }
    favorites = favorites + favoriteLog
    return true
}

fun MixShareInfo.toDataLog(): FileDataLog {
    return FileDataLog(
        shareInfoData = this.toString(),
        name = this.fileName,
        size = this.fileSize
    )
}

fun deleteFavoriteLog(uploadLog: FileDataLog, callback: () -> Unit = {}) {
    MixDialogBuilder("确定删除?").apply {
        setContent {
            Text(text = "确定从收藏记录中删除?")
        }
        setPositiveButton("确定") {
            favorites -= uploadLog
            closeDialog()
            callback()
            showToast("删除成功")
        }
        setDefaultNegative()
        show()
    }
}
