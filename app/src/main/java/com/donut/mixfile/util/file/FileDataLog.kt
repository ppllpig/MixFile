package com.donut.mixfile.util.file

import android.os.Parcelable
import androidx.compose.material3.Text
import com.donut.mixfile.server.utils.bean.MixShareInfo
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.showToast
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class FileDataLog(
    val shareInfoData: String,
    val name: String,
    val size: Long,
    val time: Date = Date(),
) : Parcelable {

    override fun hashCode(): Int {
        return shareInfoData.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileDataLog) return false
        return shareInfoData.contentEquals(other.shareInfoData)
    }
}

var uploadLogs by cachedMutableOf(listOf<FileDataLog>(), "upload_file_logs")

var favorites by cachedMutableOf(listOf<FileDataLog>(), "favorite_file_logs")

fun isFavorite(shareInfo: MixShareInfo): Boolean {
    return favorites.contains(shareInfo.toDataLog())
}

fun addFavoriteLog(shareInfo: MixShareInfo) {
    val favoriteLog = shareInfo.toDataLog()
    if (favorites.any { it == favoriteLog }) {
        favorites = favorites.filter { it != favoriteLog } + favoriteLog
        return
    }
    if (favorites.size > 1000) {
        favorites = favorites.drop(1)
    }
    favorites = favorites + favoriteLog
}

fun MixShareInfo.toDataLog(): FileDataLog {
    return FileDataLog(
        shareInfoData = this.toString(),
        name = this.fileName,
        size = this.fileSize
    )
}

fun addUploadLog(shareInfo: MixShareInfo) {
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