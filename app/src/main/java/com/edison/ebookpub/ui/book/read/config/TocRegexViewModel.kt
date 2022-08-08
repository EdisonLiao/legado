package com.edison.ebookpub.ui.book.read.config

import android.app.Application
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.TxtTocRule
import com.edison.ebookpub.help.DefaultData

class TocRegexViewModel(application: Application) : BaseViewModel(application) {

    fun saveRule(rule: TxtTocRule) {
        execute {
            if (rule.serialNumber < 0) {
                rule.serialNumber = appDb.txtTocRuleDao.maxOrder + 1
            }
            appDb.txtTocRuleDao.insert(rule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

}