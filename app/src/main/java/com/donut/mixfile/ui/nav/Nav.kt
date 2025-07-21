package com.donut.mixfile.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.ui.routes.About
import com.donut.mixfile.ui.routes.favorites.Favorites
import com.donut.mixfile.ui.routes.home.Home
import com.donut.mixfile.ui.routes.settings.MixSettings
import com.donut.mixfile.ui.routes.webdav.WebDAV
import com.donut.mixfile.util.OnDispose
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavComponent() {

    OnDispose {
        navControllerCache.clear()
    }

    val currentRoute = getCurrentRoute()
    val controller = getNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Text("菜单", modifier = Modifier.padding(16.dp), fontSize = 20.sp)
                @Composable
                fun NavButton(text: String, icon: ImageVector, jumpTo: String) {
                    val selected = jumpTo == currentRoute
                    val color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
                    NavigationDrawerItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                controller.navigate(jumpTo) {
                                    launchSingleTop = true
                                }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        label = {
                            Text(text = text, color = color)
                        },
                        icon = {
                            Icon(icon, contentDescription = text, tint = color)
                        }
                    )
                    return
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavButton("主页", Icons.Outlined.Home, Home.name)
                    NavButton("收藏", Icons.Outlined.Star, Favorites.name)
                    NavButton("设置", Icons.Outlined.Settings, MixSettings.name)
                    NavButton("WebDAV", Icons.Outlined.CastConnected, WebDAV.name)
                    NavButton("关于", Icons.Outlined.Info, About.name)
                }
            }
        },
    ) {

        fun toggleDrawer() {
            scope.launch {
                drawerState.apply {
                    if (isClosed) open() else close()
                }
            }
        }
        Scaffold(
            floatingActionButton = currentFloatingButtons,
            topBar = {
                TopAppBar(
                    modifier = Modifier.clickable {
                        toggleDrawer()
                    },
                    navigationIcon = {
                        Icon(
                            Icons.Filled.Menu,
                            tint = colorScheme.primary,
                            contentDescription = "menu",
                            modifier = Modifier
                                .size(30.dp)
                        )
                    },
                    title = {
                        Text("菜单", color = colorScheme.primary)
                    }
                )
            },
        ) { contentPadding ->
            NavContent(contentPadding)
        }
    }


}