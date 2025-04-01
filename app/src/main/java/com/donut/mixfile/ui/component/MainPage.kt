package com.donut.mixfile.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.donut.mixfile.ui.nav.NavComponent
import com.donut.mixfile.ui.routes.autoCheckUpdate
import com.donut.mixfile.ui.routes.checkForUpdates

@Composable
fun MainContent() {

    LaunchedEffect(Unit) {
        if (autoCheckUpdate) {
            checkForUpdates()
        }
    }

    NavComponent()
}