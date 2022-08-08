package com.edison.ebookpub.help

import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.HttpTTS
import com.edison.ebookpub.data.entities.KeyboardAssist
import com.edison.ebookpub.data.entities.RssSource
import com.edison.ebookpub.data.entities.TxtTocRule
import com.edison.ebookpub.help.config.ReadBookConfig
import com.edison.ebookpub.help.config.ThemeConfig
import com.edison.ebookpub.model.BookCover
import com.edison.ebookpub.utils.GSON
import com.edison.ebookpub.utils.fromJsonArray
import com.edison.ebookpub.utils.fromJsonObject
import splitties.init.appCtx
import java.io.File

object DefaultData {

    val httpTTS: List<HttpTTS> by lazy {
        val json =
            String(
                appCtx.assets.open("defaultData${File.separator}httpTTS.json")
                    .readBytes()
            )
        HttpTTS.fromJsonArray(json).getOrElse {
            emptyList()
        }
    }

    val readConfigs: List<ReadBookConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ReadBookConfig.Config>(json).getOrNull()
            ?: emptyList()
    }

    val txtTocRules: List<TxtTocRule> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}txtTocRule.json")
                .readBytes()
        )
        GSON.fromJsonArray<TxtTocRule>(json).getOrNull() ?: emptyList()
    }

    val themeConfigs: List<ThemeConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ThemeConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ThemeConfig.Config>(json).getOrNull() ?: emptyList()
    }

    val rssSources: List<RssSource> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}rssSources.json")
                .readBytes()
        )
        RssSource.fromJsonArray(json).getOrDefault(emptyList())
    }

    val coverRuleConfig: BookCover.CoverRuleConfig by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}coverRuleConfig.json")
                .readBytes()
        )
        GSON.fromJsonObject<BookCover.CoverRuleConfig>(json).getOrThrow()!!
    }

    val keyboardAssists: List<KeyboardAssist> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}keyboardAssists.json")
                .readBytes()
        )
        GSON.fromJsonArray<KeyboardAssist>(json).getOrNull()!!
    }

    fun importDefaultHttpTTS() {
        appDb.httpTTSDao.deleteDefault()
        appDb.httpTTSDao.insert(*httpTTS.toTypedArray())
    }

    fun importDefaultTocRules() {
        appDb.txtTocRuleDao.deleteDefault()
        appDb.txtTocRuleDao.insert(*txtTocRules.toTypedArray())
    }

    fun importDefaultRssSources() {
        appDb.rssSourceDao.insert(*rssSources.toTypedArray())
    }

}