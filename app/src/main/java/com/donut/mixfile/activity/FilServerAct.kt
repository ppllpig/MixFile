package com.donut.mixfile.activity

import android.os.Bundle
import com.donut.mixfile.util.objects.MixActivity

class FileServerActivity : MixActivity("file_server") {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}