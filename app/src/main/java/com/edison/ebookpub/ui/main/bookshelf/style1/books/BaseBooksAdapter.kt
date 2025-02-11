package com.edison.ebookpub.ui.main.bookshelf.style1.books

import android.content.Context
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.edison.ebookpub.base.adapter.DiffRecyclerAdapter
import com.edison.ebookpub.data.entities.Book

abstract class BaseBooksAdapter<VB : ViewBinding>(context: Context) :
    DiffRecyclerAdapter<Book, VB>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<Book>
        get() = object : DiffUtil.ItemCallback<Book>() {

            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.name == newItem.name
                        && oldItem.author == newItem.author
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return when {
                    oldItem.durChapterTime != newItem.durChapterTime -> false
                    oldItem.name != newItem.name -> false
                    oldItem.author != newItem.author -> false
                    oldItem.durChapterTitle != newItem.durChapterTitle -> false
                    oldItem.latestChapterTitle != newItem.latestChapterTitle -> false
                    oldItem.lastCheckCount != newItem.lastCheckCount -> false
                    oldItem.getDisplayCover() != newItem.getDisplayCover() -> false
                    oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum() -> false
                    else -> true
                }
            }

            override fun getChangePayload(oldItem: Book, newItem: Book): Any? {
                val bundle = bundleOf()
                if (oldItem.name != newItem.name) {
                    bundle.putString("name", newItem.name)
                }
                if (oldItem.author != newItem.author) {
                    bundle.putString("author", newItem.author)
                }
                if (oldItem.durChapterTitle != newItem.durChapterTitle) {
                    bundle.putString("dur", newItem.durChapterTitle)
                }
                if (oldItem.latestChapterTitle != newItem.latestChapterTitle) {
                    bundle.putString("last", newItem.latestChapterTitle)
                }
                if (oldItem.getDisplayCover() != newItem.getDisplayCover()) {
                    bundle.putString("cover", newItem.getDisplayCover())
                }
                if (oldItem.lastCheckCount != newItem.lastCheckCount
                    || oldItem.durChapterTime != newItem.durChapterTime
                    || oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum()
                    || oldItem.lastCheckCount != newItem.lastCheckCount
                ) {
                    bundle.putBoolean("refresh", true)
                }
                if (bundle.isEmpty) return null
                return bundle
            }

        }

    fun notification(bookUrl: String) {
        for (i in 0 until itemCount) {
            getItem(i)?.let {
                if (it.bookUrl == bookUrl) {
                    notifyItemChanged(i, bundleOf(Pair("refresh", null)))
                    return
                }
            }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun openBookInfo(book: Book)
        fun isUpdate(bookUrl: String): Boolean
    }
}