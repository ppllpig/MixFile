package com.donut.mixfile.server

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.donut.mixfile.server.core.uploaders.js.JSUploader
import com.donut.mixfile.ui.component.common.MixDialogBuilder
import com.donut.mixfile.util.cachedMutableOf


var JAVASCRIPT_UPLOADER_CODE by cachedMutableOf("", "JAVASCRIPT_UPLOADER_CODE")

val JavaScriptUploader
    get() = JSUploader(
        "JS自定义线路",
        scriptCode = JAVASCRIPT_UPLOADER_CODE
    )


fun openJavaScriptUploaderWindow() {
    MixDialogBuilder("JS自定义线路设置").apply {
        setContent {
            OutlinedTextField(
                value = JAVASCRIPT_UPLOADER_CODE,
                onValueChange = {
                    JAVASCRIPT_UPLOADER_CODE = it
                },
                minLines = 10,
                label = {
                    Text(text = "JavaScript代码")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        setDefaultNegative("关闭")
        show()
    }
}