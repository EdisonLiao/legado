package com.edison.ebookpub.ui.book.manage

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.model.webBook.WebBook
import com.edison.ebookpub.utils.toastOnUi


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {

    val batchChangeSourceState = mutableStateOf(false)
    val batchChangeSourceSize = mutableStateOf(0)
    val batchChangeSourcePosition = mutableStateOf(0)
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate)
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(vararg book: Book) {
        execute {
            appDb.bookDao.delete(*book)
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            batchChangeSourceSize.value = books.size
            books.forEachIndexed { index, book ->
                batchChangeSourcePosition.value = index + 1
                if (book.isLocalBook()) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .onFailure {
                        context.toastOnUi("获取书籍出错\n${it.localizedMessage}")
                    }.getOrNull()?.let { newBook ->
                        WebBook.getChapterListAwait(this, source, newBook)
                            .onFailure {
                                context.toastOnUi("获取目录出错\n${it.localizedMessage}")
                            }.getOrNull()?.let { toc ->
                                book.changeTo(newBook, toc)
                                appDb.bookDao.insert(newBook)
                                appDb.bookChapterDao.insert(*toc.toTypedArray())
                            }
                    }
            }
        }.onFinally {
            batchChangeSourceState.value = false
        }
    }

}