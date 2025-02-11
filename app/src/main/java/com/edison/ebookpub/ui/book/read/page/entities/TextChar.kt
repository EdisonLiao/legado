package com.edison.ebookpub.ui.book.read.page.entities

data class TextChar(
    val charData: String,
    var start: Float,
    var end: Float,
    var selected: Boolean = false,
    var isImage: Boolean = false,
    var isSearchResult: Boolean = false
) {

    fun isTouch(x: Float): Boolean {
        return x > start && x < end
    }

}