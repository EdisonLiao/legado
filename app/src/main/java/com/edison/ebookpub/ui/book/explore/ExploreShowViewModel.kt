package com.edison.ebookpub.ui.book.explore

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.data.entities.SearchBook
import com.edison.ebookpub.model.webBook.WebBook
import com.edison.ebookpub.utils.msg
import com.edison.ebookpub.utils.printOnDebug

import kotlinx.coroutines.Dispatchers.IO


class ExploreShowViewModel(application: Application) : BaseViewModel(application) {

    val booksData = MutableLiveData<List<SearchBook>>()
    val errorLiveData = MutableLiveData<String>()
    private var bookSource: BookSource? = null
    private var exploreUrl: String? = null
    private var page = 1

    fun initData(intent: Intent) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            exploreUrl = intent.getStringExtra("exploreUrl")
            if (bookSource == null && sourceUrl != null) {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            explore()
        }
    }

    fun explore() {
        val source = bookSource
        val url = exploreUrl
        if (source != null && url != null) {
            WebBook.exploreBook(viewModelScope, source, url, page)
                .timeout(30000L)
                .onSuccess(IO) { searchBooks ->
                    booksData.postValue(searchBooks)
                    appDb.searchBookDao.insert(*searchBooks.toTypedArray())
                    page++
                }.onError {
                    it.printOnDebug()
                    errorLiveData.postValue(it.msg)
                }
        }
    }

}