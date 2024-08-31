package com.donut.mixfile.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
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
import com.donut.mixfile.ui.nav.MixNavPage
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.file.deleteFavoriteLog
import com.donut.mixfile.util.file.favorites

val Favorites = MixNavPage(
    gap = 10.dp,
    horizontalAlignment = Alignment.CenterHorizontally
) {

    var searchVal by remember {
        mutableStateOf("")
    }

    var result by remember {
        mutableStateOf(favorites.reversed())
    }

    LaunchedEffect(key1 = searchVal) {
        if (searchVal.trim().isNotEmpty()) {
            result = favorites.filter {
                it.name.contains(searchVal)
            }.reversed()
        } else {
            result = favorites.reversed()
        }
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