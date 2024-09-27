package com.donut.mixfile.util.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.serverStarted
import com.donut.mixfile.ui.routes.home.tryResolveFile
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.formatTime
import com.donut.mixfile.util.parseFileMimeType
import com.donut.mixfile.util.reveiver.NetworkChangeReceiver

var filePreview by cachedMutableOf("关闭", "mix_file_preview")

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewCard(
    fileDataLog: FileDataLog,
    showDate: Boolean = true,
    longClick: () -> Unit = {},
) {
    val isImage = fileDataLog.name.parseFileMimeType().run {
        this.startsWith("image/")
    }
    val isVideo = fileDataLog.name.parseFileMimeType().run {
        this.startsWith("video/")
    }

    LaunchedEffect(updateMark) {

    }
    ElevatedCard(
        colors = CardDefaults.cardColors(
//            containerColor = Color(107, 218, 246, 0),
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .combinedClickable(
                onLongClick = {
                    longClick()
                }
            ) {
                tryResolveFile(fileDataLog.shareInfoData)
            }
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (serverStarted) {
                val shareInfo = resolveMixShareInfo(fileDataLog.shareInfoData)
                if (shareInfo != null) {
                    if (isImage && fileDataLog.size < 1024 * 1024 * 20 || isVideo) {
                        ImageContent(
                            shareInfo.downloadUrl,
                            Modifier
                                .height(200.dp)
                                .fillMaxSize(),
                            scale = ContentScale.Crop

                        )
                    }
                }
            }
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
}

@Composable
fun FileCardList(cardList: List<FileDataLog>, longClick: (FileDataLog) -> Unit = {}) {

    if (filePreview.contentEquals("开启") ||
        (filePreview.contentEquals("仅Wifi") && NetworkChangeReceiver.isWifi)
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 1000.dp),
            columns = GridCells.Fixed(2),
        ) {
            items(cardList.size) { index ->
                PreviewCard(cardList[index]) {
                    deleteFavoriteLog(cardList[index])
                }
            }
        }
        return
    }
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
            items(cardList.size) { index ->
                FileCard(cardList[index], longClick = longClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileCard(
    fileDataLog: FileDataLog,
    showDate: Boolean = true,
    longClick: (FileDataLog) -> Unit = {},
) {
    LaunchedEffect(updateMark) {

    }
    HorizontalDivider()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(107, 218, 246, 0),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    longClick(fileDataLog)
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