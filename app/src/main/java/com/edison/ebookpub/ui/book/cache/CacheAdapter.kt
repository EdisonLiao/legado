package com.edison.ebookpub.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.edison.ebookpub.R
import com.edison.ebookpub.base.adapter.ItemViewHolder
import com.edison.ebookpub.base.adapter.RecyclerAdapter
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.databinding.ItemDownloadBinding
import com.edison.ebookpub.model.CacheBook
import com.edison.ebookpub.utils.gone
import com.edison.ebookpub.utils.visible

class CacheAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<Book, ItemDownloadBinding>(context) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()

    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                tvName.text = item.name
                tvAuthor.text = context.getString(R.string.author_show, item.getRealAuthor())
                if (item.isLocalBook()) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cs = cacheChapters[item.bookUrl]
                    if (cs == null) {
                        tvDownload.setText(R.string.loading)
                    } else {
                        tvDownload.text =
                            context.getString(
                                R.string.download_count,
                                cs.size,
                                item.totalChapterNum
                            )
                    }
                }
            } else {
                if (item.isLocalBook()) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cacheSize = cacheChapters[item.bookUrl]?.size ?: 0
                    tvDownload.text =
                        context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                }
            }
            upDownloadIv(ivDownload, item)
            upExportInfo(tvMsg, progressExport, item)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDownloadBinding) {
        binding.run {
            ivDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let { book ->
                    CacheBook.cacheBookMap[book.bookUrl]?.let {
                        if (!it.isStop()) {
                            CacheBook.remove(context, book.bookUrl)
                        } else {
                            CacheBook.start(context, book, 0, book.totalChapterNum)
                        }
                    } ?: let {
                        CacheBook.start(context, book, 0, book.totalChapterNum)
                    }
                }
            }
            tvExport.setOnClickListener {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun upDownloadIv(iv: ImageView, book: Book) {
        if (book.isLocalBook()) {
            iv.gone()
        } else {
            iv.visible()
            CacheBook.cacheBookMap[book.bookUrl]?.let {
                if (!it.isStop()) {
                    iv.setImageResource(R.drawable.ic_stop_black_24dp)
                } else {
                    iv.setImageResource(R.drawable.ic_play_24dp)
                }
            } ?: let {
                iv.setImageResource(R.drawable.ic_play_24dp)
            }
        }
    }

    private fun upExportInfo(msgView: TextView, progressView: ProgressBar, book: Book) {
        val msg = callBack.exportMsg(book.bookUrl)
        if (msg != null) {
            msgView.text = msg
            msgView.visible()
            progressView.gone()
            return
        }
        msgView.gone()
        val progress = callBack.exportProgress(book.bookUrl)
        if (progress != null) {
            progressView.max = book.totalChapterNum
            progressView.progress = progress
            progressView.visible()
            return
        }
        progressView.gone()
    }

    interface CallBack {
        fun export(position: Int)
        fun exportProgress(bookUrl: String): Int?
        fun exportMsg(bookUrl: String): String?
    }
}