package com.donut.mixfile.util.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.server.core.utils.parseFileMimeType
import com.donut.mixfile.server.serverStarted
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.UseEffect
import com.donut.mixfile.util.cachedMutableOf
import com.donut.mixfile.util.formatFileSize
import com.donut.mixfile.util.formatTime
import com.donut.mixfile.util.reveiver.NetworkChangeReceiver
import kotlinx.coroutines.delay

var filePreview by cachedMutableOf("关闭", "mix_file_preview")

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewCard(
    fileDataLog: FileDataLog,
    showDate: Boolean = true,
    longClick: (FileDataLog) -> Unit = {},
    selected: Boolean,
    onClick: (FileDataLog) -> Unit
) {
    val isImage = fileDataLog.name.parseFileMimeType().run {
        this.contentType.contentEquals("image")
    }

    val isVideo = fileDataLog.name.parseFileMimeType().run {
        this.contentType.contentEquals("video")
    }

    LaunchedEffect(favorites) {

    }

    val color = remember(selected) {
        if (selected)
            Color(107, 184, 242, 84)
        else
            Color(107, 218, 246, 0)
    }

    ElevatedCard(
//        colors = CardDefaults.cardColors(
//            contentColor = color,
//        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .combinedClickable(
                onLongClick = {
                    longClick(fileDataLog)
                }
            ) {
                onClick(fileDataLog)
            }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color),
            verticalArrangement = Arrangement.Bottom
        ) {
            var loadImg by remember { mutableStateOf(false) }
            UseEffect {
                delay(300)
                loadImg = true
            }
            if (serverStarted) {
                if (isImage && fileDataLog.size < 1024 * 1024 * 20 || isVideo) {
                    if (loadImg) {
                        ImageContent(
                            fileDataLog.downloadUrl,
                            Modifier
                                .height(200.dp)
                                .fillMaxSize(),
                            scale = ContentScale.Crop

                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxSize()
                        ) {

                        }
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
fun FileCardList(
    cardList: List<FileDataLog>,
    selected: Set<FileDataLog> = setOf(),
    onClick: (FileDataLog) -> Unit = {
        showFileInfoDialog(it)
    },
    longClick: (FileDataLog) -> Unit = {},
) {
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
                val log = cardList[index]
                PreviewCard(
                    log,
                    onClick = onClick,
                    longClick = longClick,
                    selected = selected.contains(log)
                )
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
                val log = cardList[index]
                if (index > 0) {
                    HorizontalDivider()
                }
                FileCard(
                    log,
                    longClick = longClick,
                    selected = selected.contains(log),
                    onClick = onClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FileCard(
    fileDataLog: FileDataLog,
    showDate: Boolean = true,
    onClick: (FileDataLog) -> Unit,
    selected: Boolean = false,
    longClick: (FileDataLog) -> Unit = {},
) {
    LaunchedEffect(favorites) {

    }
    val color = remember(selected) {
        if (selected)
            Color(107, 184, 242, 84)
        else
            Color(107, 218, 246, 0)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .combinedClickable(
                onLongClick = {
                    longClick(fileDataLog)
                }
            ) {
                onClick(fileDataLog)
            }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "selected",
                        tint = colorScheme.primary
                    )
                }
                Text(
                    text = fileDataLog.name.trim(),
                    color = colorScheme.primary,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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