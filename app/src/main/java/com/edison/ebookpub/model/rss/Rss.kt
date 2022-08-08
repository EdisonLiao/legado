package com.edison.ebookpub.model.rss

import com.edison.ebookpub.data.entities.RssArticle
import com.edison.ebookpub.data.entities.RssSource
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.model.Debug
import com.edison.ebookpub.model.analyzeRule.AnalyzeRule
import com.edison.ebookpub.model.analyzeRule.AnalyzeUrl
import com.edison.ebookpub.model.analyzeRule.RuleData
import com.edison.ebookpub.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
object Rss {

    fun getArticles(
        scope: CoroutineScope,
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<Pair<MutableList<RssArticle>, String?>> {
        return Coroutine.async(scope, context) {
            getArticlesAwait(sortName, sortUrl, rssSource, page)
        }
    }

    suspend fun getArticlesAwait(
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
    ): Pair<MutableList<RssArticle>, String?> {
        val ruleData = RuleData()
        val analyzeUrl = AnalyzeUrl(
            sortUrl,
            page = page,
            source = rssSource,
            ruleData = ruleData,
            headerMapF = rssSource.getHeaderMap()
        )
        val body = analyzeUrl.getStrResponseAwait().body
        return RssParserByRule.parseXML(sortName, sortUrl, body, rssSource, ruleData)
    }

    fun getContent(
        scope: CoroutineScope,
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentAwait(rssArticle, ruleContent, rssSource)
        }
    }

    suspend fun getContentAwait(
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
    ): String {
        val analyzeUrl = AnalyzeUrl(
            rssArticle.link,
            baseUrl = rssArticle.origin,
            source = rssSource,
            ruleData = rssArticle,
            headerMapF = rssSource.getHeaderMap()
        )
        val body = analyzeUrl.getStrResponseAwait().body
        Debug.log(rssSource.sourceUrl, "≡获取成功:${rssSource.sourceUrl}")
        Debug.log(rssSource.sourceUrl, body, state = 20)
        val analyzeRule = AnalyzeRule(rssArticle, rssSource)
        analyzeRule.setContent(body)
            .setBaseUrl(NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link))
        return analyzeRule.getString(ruleContent)
    }
}