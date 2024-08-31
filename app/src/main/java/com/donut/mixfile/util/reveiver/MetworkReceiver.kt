package com.donut.mixfile.util.reveiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.donut.mixfile.server.serverPort
import com.donut.mixfile.ui.routes.serverAddress
import com.donut.mixfile.util.getIpAddressInLocalNetwork

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        serverAddress = "http://${getIpAddressInLocalNetwork()}:$serverPort"
    }
}