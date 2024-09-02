package com.donut.mixfile.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.donut.mixfile.currentActivity
import com.donut.mixfile.ui.component.common.CommonColumn
import com.donut.mixfile.ui.theme.MainTheme
import com.donut.mixfile.ui.theme.colorScheme
import com.donut.mixfile.util.file.resolveMixShareInfo
import com.donut.mixfile.util.file.showFileShareDialog
import com.donut.mixfile.util.objects.MixActivity

class FileDialogActivity : MixActivity("file_dialog") {

    @Composable
    fun DialogContainer(content: @Composable () -> Unit) {

        Dialog(
            onDismissRequest = {
                currentActivity.finish()
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {

            Card(
                modifier = Modifier
                    .systemBarsPadding()
                    .heightIn(200.dp, 600.dp)
            ) {
                CommonColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start,
                ) {
                    content()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shareData = intent.data?.host ?: ""
        val shareInfo = resolveMixShareInfo(shareData)
        setContent {
            MainTheme {
                LaunchedEffect(Unit) {
                    if (shareInfo != null) {
                        showFileShareDialog(shareInfo) {
                            finish()
                        }
                    }
                }
                if (shareInfo == null){
                    DialogContainer {
                        Text(
                            text = "无效分享码",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}