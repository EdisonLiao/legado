package com.edison.ebookpub.ui.book.bookmark

import android.app.Application
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Bookmark

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    fun initData(onSuccess: (bookmarks: List<Bookmark>) -> Unit) {
        execute {
            appDb.bookmarkDao.all
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        execute {
            appDb.bookmarkDao.delete(bookmark)
        }
    }

}