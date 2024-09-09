package com.donut.mixfile

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import com.donut.mixfile.server.FileService
import com.donut.mixfile.ui.component.MainContent
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.util.file.MixFileSelector
import com.donut.mixfile.util.objects.MixActivity
import com.donut.mixfile.util.reveiver.NetworkChangeReceiver

class MainActivity : MixActivity(MAIN_ID) {

    companion object {
        lateinit var mixFileSelector: MixFileSelector
        lateinit var networkChangeReceiver: NetworkChangeReceiver
    }

    override fun onDestroy() {
        super.onDestroy()
        mixFileSelector.unregister()
        unregisterReceiver(networkChangeReceiver)
        startService(Intent(this, FileService::class.java))
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        mixFileSelector = MixFileSelector(this)
        super.onCreate(savedInstanceState)
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        networkChangeReceiver = NetworkChangeReceiver()

        //请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        registerReceiver(networkChangeReceiver, intentFilter)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                MainContent()
            }
        }
    }
}

