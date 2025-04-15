package com.donut.mixfile.ui.nav

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import com.donut.mixfile.ui.routes.About
import com.donut.mixfile.ui.routes.MixSettings
import com.donut.mixfile.ui.routes.favorites.Favorites
import com.donut.mixfile.ui.routes.home.Home
import com.donut.mixfile.ui.routes.webdav.WebDAV

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavContent(innerPaddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPaddingValues)
            .fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val controller = getNavController()

        NavHost(
            navController = controller,
            startDestination = Home.name
        ) {
            Home(this)
            MixSettings(this)
            About(this)
            Favorites(this)
            WebDAV(this)
        }
    }
}