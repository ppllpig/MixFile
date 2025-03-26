package com.donut.mixfile.server

import com.donut.mixfile.server.core.utils.bean.MixShareInfo
import com.donut.mixfile.ui.routes.home.getLocalServerAddress
import com.donut.mixfile.ui.routes.home.serverAddress
import com.donut.mixfile.util.getFileAccessUrl

val MixShareInfo.downloadUrl: String
    get() = getFileAccessUrl(getLocalServerAddress(), this.toString(), fileName)


val MixShareInfo.lanUrl: String
    get() = getFileAccessUrl(serverAddress, this.toString(), fileName)