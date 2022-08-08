package com.edison.ebookpub.ui.main.explore

import androidx.recyclerview.widget.DiffUtil
import com.edison.ebookpub.data.entities.BookSource


class ExploreDiffItemCallBack : DiffUtil.ItemCallback<BookSource>() {

    override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
        if (oldItem.bookSourceName != newItem.bookSourceName) {
            return false
        }
        return true
    }

}