package com.donut.mixfile.util.file

import androidx.compose.material3.Text
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.ui.routes.autoAddFavorite
import com.donut.mixfile.ui.routes.currentCategory
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.showToast
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.Date


class TimestampAdapter : TypeAdapter<Date?>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.value(value.time)
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Date? {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return Date(`in`.nextLong())
    }
}

data class FileDataLog(
    val shareInfoData: String,
    val name: String,
    val size: Long,
    @JsonAdapter(TimestampAdapter::class)
    val time: Date = Date(),
    var category: String = "默认",
) {

    init {
        //限制category长度
        if (category.length > 20) {
            category = category.substring(0, 20)
        }
        category = category.trim()
    }

    override fun hashCode(): Int {
        return shareInfoData.hashCode()
    }

    fun updateCategory(category: String) {
        favorites = favorites.toMutableList().apply {
            remove(this@FileDataLog)
        }
        this.category = category
        favorites = favorites + this.copy()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileDataLog) return false
        return shareInfoData.contentEquals(other.shareInfoData)
    }
}

var uploadLogs by cachedMutableOf(listOf<FileDataLog>(), "upload_file_logs")

var favorites by cachedMutableOf(listOf<FileDataLog>(), "favorite_file_logs")

var favCategories by cachedMutableOf(setOf("默认"), "fav_categories")

fun isFavorite(shareInfo: MixShareInfo): Boolean {
    return favorites.contains(shareInfo.toDataLog())
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
        favorites = favorites.filter { it != favoriteLog } + favoriteLog
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

fun deleteFavoriteLog(uploadLog: FileDataLog, callback: () -> Unit = {}) {
    MixDialogBuilder("确定删除?").apply {
        setContent {
            Text(text = "确定从收藏记录中删除?")
        }
        setPositiveButton("确定") {
            favorites = favorites.filter { it != uploadLog }
            closeDialog()
            callback()
            showToast("删除成功")
        }
        setDefaultNegative()
        show()
    }
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