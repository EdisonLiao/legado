package com.edison.ebookpub.ui.main.explore

import android.app.Application
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookSource

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

}