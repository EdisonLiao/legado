package com.edison.ebookpub.ui.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.constant.AppConst
import com.edison.ebookpub.constant.AppLog
import com.edison.ebookpub.constant.BookType
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.help.AppWebDav
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.help.DefaultData
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.LocalConfig
import com.edison.ebookpub.model.CacheBook
import com.edison.ebookpub.model.analyzeRule.AnalyzeRule
import com.edison.ebookpub.model.webBook.WebBook
import com.edison.ebookpub.service.CacheBookService
import com.edison.ebookpub.utils.postEvent
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class MainViewModel(application: Application) : BaseViewModel(application) {
    private var threadCount = AppConfig.threadCount
    private var upTocPool =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private val waitUpTocBooks = arrayListOf<String>()
    private val onUpTocBooks = CopyOnWriteArraySet<String>()
    private var upTocJob: Job? = null
    private var cacheBookJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        upTocPool.close()
    }

    fun upPool() {
        threadCount = AppConfig.threadCount
        upTocPool.close()
        upTocPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    }

    fun isUpdate(bookUrl: String): Boolean {
        return onUpTocBooks.contains(bookUrl)
    }

    fun upAllBookToc() {
        execute {
            addToWaitUp(appDb.bookDao.hasUpdateBooks)
        }
    }

    fun upToc(books: List<Book>) {
        execute(context = upTocPool) {
            books.filter {
                it.origin != BookType.local && it.canUpdate
            }.let {
                addToWaitUp(it)
            }
        }
    }

    @Synchronized
    private fun addToWaitUp(books: List<Book>) {
        books.forEach { book ->
            if (!waitUpTocBooks.contains(book.bookUrl) && !onUpTocBooks.contains(book.bookUrl)) {
                waitUpTocBooks.add(book.bookUrl)
            }
        }
        if (upTocJob == null) {
            startUpTocJob()
        }
    }

    private fun startUpTocJob() {
        upTocJob = viewModelScope.launch(upTocPool) {
            while (isActive) {
                when {
                    waitUpTocBooks.isEmpty() -> {
                        upTocJob?.cancel()
                        upTocJob = null
                    }
                    onUpTocBooks.size < threadCount -> {
                        updateToc()
                    }
                    else -> {
                        delay(500)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun updateToc() {
        val bookUrl = waitUpTocBooks.firstOrNull() ?: return
        if (onUpTocBooks.contains(bookUrl)) {
            waitUpTocBooks.remove(bookUrl)
            return
        }
        val book = appDb.bookDao.getBook(bookUrl)
        if (book == null) {
            waitUpTocBooks.remove(bookUrl)
            return
        }
        val source = appDb.bookSourceDao.getBookSource(book.origin)
        if (source == null) {
            waitUpTocBooks.remove(book.bookUrl)
            return
        }
        waitUpTocBooks.remove(bookUrl)
        upTocAdd(bookUrl)
        execute(context = upTocPool) {
            val oldBook = book.copy()
            val preUpdateJs = source.ruleToc?.preUpdateJs
            if (!preUpdateJs.isNullOrBlank()) {
                AnalyzeRule(book, source).evalJS(preUpdateJs)
            }
            if (book.tocUrl.isBlank()) {
                WebBook.getBookInfoAwait(this, source, book)
            }
            val toc = WebBook.getChapterListAwait(this, source, book).getOrThrow()
            if (book.bookUrl == bookUrl) {
                appDb.bookDao.update(book)
            } else {
                upTocAdd(book.bookUrl)
                appDb.bookDao.insert(book)
                BookHelp.updateCacheFolder(oldBook, book)
            }
            appDb.bookChapterDao.delByBook(bookUrl)
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            addDownload(source, book)
        }.onError(upTocPool) {
            AppLog.put("${book.name} 更新目录失败\n${it.localizedMessage}", it)
        }.onCancel(upTocPool) {
            upTocCancel(bookUrl)
            upTocCancel(book.bookUrl)
        }.onFinally(upTocPool) {
            upTocFinally(bookUrl)
            upTocFinally(book.bookUrl)
        }
    }

    @Synchronized
    private fun upTocAdd(bookUrl: String) {
        onUpTocBooks.add(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    @Synchronized
    private fun upTocCancel(bookUrl: String) {
        onUpTocBooks.remove(bookUrl)
        waitUpTocBooks.add(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    @Synchronized
    private fun upTocFinally(bookUrl: String) {
        waitUpTocBooks.remove(bookUrl)
        onUpTocBooks.remove(bookUrl)
        postEvent(EventBus.UP_BOOKSHELF, bookUrl)
    }

    @Synchronized
    private fun addDownload(source: BookSource, book: Book) {
        val endIndex = min(
            book.totalChapterNum - 1,
            book.durChapterIndex.plus(AppConfig.preDownloadNum)
        )
        val cacheBook = CacheBook.getOrCreate(source, book)
        cacheBook.addDownload(book.durChapterIndex, endIndex)
        if (cacheBookJob == null && !CacheBookService.isRun) {
            cacheBook()
        }
    }

    /**
     * 缓存书籍
     */
    private fun cacheBook() {
        cacheBookJob?.cancel()
        cacheBookJob = viewModelScope.launch(upTocPool) {
            while (isActive) {
                if (CacheBookService.isRun || !CacheBook.isRun) {
                    cacheBookJob?.cancel()
                    cacheBookJob = null
                    return@launch
                }
                CacheBook.cacheBookMap.forEach {
                    val cacheBookModel = it.value
                    while (cacheBookModel.waitCount > 0) {
                        if (CacheBook.onDownloadCount < threadCount) {
                            cacheBookModel.download(this, upTocPool)
                        } else {
                            delay(100)
                        }
                    }
                }
            }
        }
    }

    fun postLoad() {
        execute {
            if (appDb.httpTTSDao.count == 0) {
                DefaultData.httpTTS.let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
        }
    }

    fun restoreWebDav(name: String) {
        execute {
            AppWebDav.restoreWebDav(name)
        }
    }

    fun upVersion() {
        execute {
            if (LocalConfig.needUpHttpTTS) {
                DefaultData.importDefaultHttpTTS()
            }
            if (LocalConfig.needUpTxtTocRule) {
                DefaultData.importDefaultTocRules()
            }
            if (LocalConfig.needUpRssSources) {
                DefaultData.importDefaultRssSources()
            }
        }
    }
}