package com.edison.ebookpub.model

import android.content.Context
import com.edison.ebookpub.constant.IntentAction
import com.edison.ebookpub.service.DownloadService
import com.edison.ebookpub.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}