package com.edison.ebookpub.ui.book.local.rule

import android.app.Application
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.TxtTocRule
import com.edison.ebookpub.help.DefaultData
import com.edison.ebookpub.help.http.newCallResponseBody
import com.edison.ebookpub.help.http.okHttpClient
import com.edison.ebookpub.help.http.text
import com.edison.ebookpub.utils.GSON
import com.edison.ebookpub.utils.fromJsonArray

class TxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    fun save(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.insert(txtTocRule)
        }
    }

    fun del(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(*txtTocRule)
        }
    }

    fun update(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.update(*txtTocRule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                GSON.fromJsonArray<TxtTocRule>(json).getOrThrow()?.let {
                    appDb.txtTocRuleDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败\n${it.localizedMessage}")
        }
    }

    fun toTop(vararg rules: TxtTocRule) {
        execute {
            val minOrder = appDb.txtTocRuleDao.minOrder - 1
            rules.forEachIndexed { index, source ->
                source.serialNumber = minOrder - index
            }
            appDb.txtTocRuleDao.update(*rules)
        }
    }

    fun toBottom(vararg sources: TxtTocRule) {
        execute {
            val maxOrder = appDb.txtTocRuleDao.maxOrder + 1
            sources.forEachIndexed { index, source ->
                source.serialNumber = maxOrder + index
            }
            appDb.txtTocRuleDao.update(*sources)
        }
    }

    fun upOrder() {
        execute {
            val sources = appDb.txtTocRuleDao.all
            for ((index: Int, source: TxtTocRule) in sources.withIndex()) {
                source.serialNumber = index + 1
            }
            appDb.txtTocRuleDao.update(*sources.toTypedArray())
        }
    }

}