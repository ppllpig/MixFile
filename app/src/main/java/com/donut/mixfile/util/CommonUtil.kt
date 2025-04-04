package com.donut.mixfile.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import com.donut.mixfile.app
import com.donut.mixfile.appScope
import com.donut.mixfile.server.core.utils.genRandomString
import com.donut.mixfile.server.core.utils.ignoreError
import com.donut.mixfile.server.core.utils.isFalse
import com.donut.mixfile.server.mixFileServer
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.EOFException
import java.math.BigInteger
import java.net.NetworkInterface
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.log10
import kotlin.math.pow

fun String.copyToClipboard(showToast: Boolean = true) {
    val clipboard = getClipBoard()
    val clip = ClipData.newPlainText("Copied Text", this)
    clipboard.setPrimaryClip(clip)
    if (showToast) showToast("复制成功")
}

fun getClipBoard(context: Context = app.applicationContext): ClipboardManager {
    return context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

fun readClipBoardText(): String {
    val clipboard = getClipBoard()
    val clip = clipboard.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).text
        return text?.toString() ?: ""
    }
    return ""
}


fun formatFileSize(bytes: Long, mb: Boolean = false): String {
    if (bytes <= 0) return "0 B"
    if (mb && bytes > 1024 * 1024) {
        return String.format(
            Locale.US,
            "%.2f MB",
            bytes / 1024.0 / 1024.0
        )
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

    return String.format(
        Locale.US,
        "%.2f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units.getOrNull(digitGroups)
    )
}

fun getAppVersion(context: Context): Pair<String, Long> {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: ""
        val versionCode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
        Pair(versionName, versionCode)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        Pair("Unknown", -1L)
    }
}


class CachedDelegate<T>(val getKeys: () -> Array<Any?>, private val initializer: () -> T) {
    private var cache: T? = null
    private var keys: Array<Any?> = arrayOf()

    operator fun getValue(thisRef: Any?, property: Any?): T {
        val newKeys = getKeys()
        if (cache == null || !keys.contentEquals(newKeys)) {
            keys = newKeys
            cache = initializer()
        }
        return cache!!
    }

    operator fun setValue(thisRef: Any?, property: Any?, value: T) {
        cache = value
    }
}


inline fun String.isUrl(block: (URL) -> Unit = {}): Boolean {
    val urlPattern =
        Regex("^https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)\$")
    val result = urlPattern.matches(this)
    if (result) {
        ignoreError {
            block(URL(this))
        }
    }
    return result
}

fun getUrlHost(url: String): String? {
    url.isUrl {
        return it.host
    }
    return null
}

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeToBase64() = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64() = Base64.decode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64String() = Base64.decode(this).decodeToString()

fun String.encodeToBase64() = this.toByteArray().encodeToBase64()

fun <T> List<T>.at(index: Long): T {
    var fixedIndex = index % this.size
    if (fixedIndex < 0) {
        fixedIndex += this.size
    }
    return this[fixedIndex.toInt()]
}

fun String.parseSortNum(): BigInteger {
    val digits = StringBuilder()

    for (char in this) {
        if (char.isDigit()) {
            digits.append(char)
        }
    }
    return if (digits.isEmpty()) {
        BigInteger.ZERO
    } else {
        BigInteger(digits.toString())
    }
}

fun Iterable<String>.sortByName(): List<String> {
    return sortedBy { it.parseSortNum() }
}

fun <T> List<T>.at(index: Int): T {
    return this.at(index.toLong())
}

fun getAppVersionName(context: Context): String? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

infix fun <T> List<T>.elementEquals(other: List<T>): Boolean {
    if (this.size != other.size) return false

    val tracker = BooleanArray(this.size)
    var counter = 0

    root@ for (value in this) {
        destination@ for ((i, o) in other.withIndex()) {
            if (tracker[i]) {
                continue@destination
            } else if (value?.equals(o) == true) {
                counter++
                tracker[i] = true
                continue@root
            }
        }
    }

    return counter == this.size
}


fun debug(text: String?, tag: String = "test") {
    Log.d(tag, text ?: "null")
}

inline fun catchError(tag: String = "", onError: () -> Unit = {}, block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        showError(e, tag)
    }
}

fun getCurrentDate(reverseDays: Long = 0): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(System.currentTimeMillis() - (reverseDays * 86400 * 1000)))
}

fun getCurrentTime(): String {
    val currentTime = Date()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return formatter.format(currentTime)
}

fun genRandomHexString(length: Int = 32) = genRandomString(length, ('0'..'9') + ('a'..'f'))

fun readRawFile(id: Int) = app.resources.openRawResource(id).readBytes()


fun isValidUri(uriString: String): Boolean {
    try {
        val uri = Uri.parse(uriString)
        return uri != null && uri.scheme != null
    } catch (e: Exception) {
        return false
    }
}

fun showError(e: Throwable, tag: String = "") {
    Log.e(
        "error",
        "${tag}发生错误: ${e.message} ${e.stackTraceToString()}"
    )
}

fun getFileAccessUrl(
    host: String = getLocalServerAddress(),
    shareInfo: String,
    fileName: String
): String {
    return "${host}/api/download?s=${
        URLEncoder.encode(
            shareInfo,
            "UTF-8"
        )
    }&accessKey=${mixFileServer.accessKey}#${fileName}"
}

fun getIpAddressInLocalNetwork(): String {
    val networkInterfaces = NetworkInterface.getNetworkInterfaces().iterator().asSequence()
    val localAddresses = networkInterfaces.flatMap {
        it.inetAddresses.asSequence()
            .filter { inetAddress ->
                inetAddress.isSiteLocalAddress && inetAddress?.hostAddress?.contains(":")
                    .isFalse() &&
                        inetAddress.hostAddress != "127.0.0.1"
            }
            .map { inetAddress -> inetAddress.hostAddress }
    }
    return localAddresses.firstOrNull() ?: "127.0.0.1"
}

inline fun errorDialog(title: String, block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        when (e) {
            is CancellationException,
            is EOFException,
                -> return
        }
        appScope.launch(Dispatchers.Main) {
            showErrorDialog(e, title)
        }
    }
}

fun formatTime(date: Date, format: String = "yyyy-MM-dd HH:mm:ss"): String {
    val formatter = SimpleDateFormat(format, Locale.US)
    return formatter.format(date)
}

fun Uri.getFileName(): String {
    var fileName = ""
    app.contentResolver.query(this, null, null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        fileName = it.getString(nameIndex)
    }
    return fileName
}

fun Uri.getFileSize() =
    app.contentResolver.openAssetFileDescriptor(this, "r")?.use { it.length } ?: 0