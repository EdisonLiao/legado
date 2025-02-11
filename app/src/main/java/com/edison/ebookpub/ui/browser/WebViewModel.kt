package com.edison.ebookpub.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.constant.AppConst
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.help.CacheManager
import com.edison.ebookpub.help.IntentData
import com.edison.ebookpub.help.http.newCallResponseBody
import com.edison.ebookpub.help.http.okHttpClient
import com.edison.ebookpub.model.analyzeRule.AnalyzeUrl
import com.edison.ebookpub.utils.*
import java.io.File
import java.util.*

class WebViewModel(application: Application) : BaseViewModel(application) {
    var baseUrl: String = ""
    var html: String? = null
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var sourceOrigin: String = ""

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            val headerMapF = IntentData.get<Map<String, String>>(url)
            val analyzeUrl = AnalyzeUrl(url, headerMapF = headerMapF)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            @Suppress("BlockingMethodInNonBlockingContext")
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun saveVerificationResult(success: () -> Unit) {
        execute {
            if (sourceVerificationEnable) {
                val key = "${sourceOrigin}_verificationResult"
                html = AnalyzeUrl(baseUrl, headerMapF = headerMap).getStrResponseAwait(useWebView = false).body
                CacheManager.putMemory(key, html ?: "")
            }
        }.onSuccess {
            success.invoke()
        }
    }

}