package com.edison.ebookpub.ui.book.read.page.api

import com.edison.ebookpub.model.ReadBook
import com.edison.ebookpub.ui.book.read.page.entities.TextChapter

interface DataSource {

    val pageIndex: Int get() = ReadBook.durPageIndex

    val currentChapter: TextChapter?

    val nextChapter: TextChapter?

    val prevChapter: TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    fun upContent(relativePosition: Int = 0, resetPageOffset: Boolean = true)
}