package com.edison.ebookpub.help

import com.edison.ebookpub.constant.AppLog
import com.edison.ebookpub.data.entities.BaseSource
import com.edison.ebookpub.exception.NoStackTraceException
import com.edison.ebookpub.ui.association.VerificationCodeActivity
import com.edison.ebookpub.ui.browser.WebViewActivity
import com.edison.ebookpub.utils.startActivity
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx


object SourceVerificationHelp {

    private var key: String = ""
    /** 
     * 获取书源验证结果
     * 图片验证码 防爬 滑动验证码 点击字符 等等
     */
    fun getVerificationResult(source: BaseSource?, url: String, title: String, useBrowser: Boolean): String {
        source ?: throw NoStackTraceException("getVerificationResult parameter source cannot be null")
        return runBlocking {
            key = "${source.getKey()}_verificationResult"
            CacheManager.delete(key)

            if (!useBrowser) {
                appCtx.startActivity<VerificationCodeActivity> {
                    putExtra("imageUrl", url)
                    putExtra("sourceOrigin", source.getKey())
                }
            } else {
                startBrowser(source, url, title, true)
            }

            var waitUserInput = false
            while(CacheManager.get(key) == null) {
                if (!waitUserInput) {
                    AppLog.putDebug("等待返回验证结果...")
                    waitUserInput = true
                }
            }
            CacheManager.get(key)!!.let {
                it.ifBlank {
                    throw NoStackTraceException("验证结果为空")
                }
            }
       }
    }

    /**
     * 启动内置浏览器
      @param saveResult 保存网页源代码到数据库
     */
    fun startBrowser(source: BaseSource?, url: String, title: String, saveResult: Boolean? = false) {
        source ?: throw NoStackTraceException("startBrowser parameter source cannot be null")
        key = "${source.getKey()}_verificationResult"
        appCtx.startActivity<WebViewActivity> {
            putExtra("title", title)
            putExtra("url", url)
            putExtra("sourceOrigin", source.getKey())
            putExtra("sourceVerificationEnable", saveResult)
            IntentData.put(url, source.getHeaderMap(true))
        }
    }

    fun checkResult() {
        CacheManager.get(key) ?: CacheManager.putMemory(key, "")
    }
}