package com.edison.ebookpub.help

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.data.entities.RssSource
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.http.cronet.CronetLoader
import com.edison.ebookpub.utils.EncoderUtils
import com.edison.ebookpub.utils.NetworkUtils
import com.edison.ebookpub.utils.splitNotBlank
import com.edison.ebookpub.utils.toastOnUi
import org.json.JSONObject
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.InputStreamReader

object SourceHelp {

    private val handler = Handler(Looper.getMainLooper())
    private val list18Plus by lazy {
        try {
            return@lazy String(appCtx.assets.open("18PlusList.txt").readBytes())
                .splitNotBlank("\n")
        } catch (e: Exception) {
            return@lazy arrayOf<String>()
        }
    }

    fun insertRssSource(vararg rssSources: RssSource) {
        rssSources.forEach { rssSource ->
            if (is18Plus(rssSource.sourceUrl)) {
                handler.post {
                    appCtx.toastOnUi("${rssSource.sourceName}是18+网址,禁止导入.")
                }
            } else {
                appDb.rssSourceDao.insert(rssSource)
            }
        }
    }

    fun insertBookSource(vararg bookSources: BookSource) {
        bookSources.forEach { bookSource ->
            appDb.bookSourceDao.insert(bookSource)
        }
    }

    private fun is18Plus(url: String?): Boolean {
        url ?: return false
        val baseUrl = NetworkUtils.getBaseUrl(url)
        baseUrl ?: return false
        if (AppConfig.isGooglePlay) return false
        try {
            val host = baseUrl.split("//", ".")
            val base64Url = EncoderUtils.base64Encode("${host[host.lastIndex - 1]}.${host.last()}")
            list18Plus.forEach {
                if (base64Url == it) {
                    return true
                }
            }
        } catch (e: Exception) {
        }
        return false
    }

    suspend fun readAssetsJsonFile(ctx: Context,fileName: String): String{
        val stringBuilder = StringBuilder()
        return try {
            //获取assets资源管理器
            val assetManager = ctx.assets
            //通过管理器打开文件并读取
            val bf = BufferedReader(
                InputStreamReader(
                    assetManager.open(fileName)
                )
            )
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            stringBuilder.toString()
        } catch (e: java.lang.Exception) {
            return ""
        }
    }

}