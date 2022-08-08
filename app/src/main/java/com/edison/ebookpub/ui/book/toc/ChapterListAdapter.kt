package com.edison.ebookpub.ui.book.toc

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.edison.ebookpub.R
import com.edison.ebookpub.base.adapter.DiffRecyclerAdapter
import com.edison.ebookpub.base.adapter.ItemViewHolder
import com.edison.ebookpub.data.entities.Book
import com.edison.ebookpub.data.entities.BookChapter
import com.edison.ebookpub.databinding.ItemChapterListBinding
import com.edison.ebookpub.help.ContentProcessor
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.lib.theme.ThemeUtils
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.utils.getCompatColor
import com.edison.ebookpub.utils.gone
import com.edison.ebookpub.utils.longToastOnUi
import com.edison.ebookpub.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ChapterListAdapter(context: Context, val callback: Callback) :
    DiffRecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    val cacheFileNames = hashSetOf<String>()
    private val displayTitleMap = ConcurrentHashMap<String, String>()

    override val diffItemCallback: DiffUtil.ItemCallback<BookChapter>
        get() = object : DiffUtil.ItemCallback<BookChapter>() {

            override fun areItemsTheSame(
                oldItem: BookChapter,
                newItem: BookChapter
            ): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(
                oldItem: BookChapter,
                newItem: BookChapter
            ): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
                        && oldItem.url == newItem.url
                        && oldItem.isVip == newItem.isVip
                        && oldItem.isPay == newItem.isPay
                        && oldItem.title == newItem.title
                        && oldItem.tag == newItem.tag
                        && oldItem.isVolume == newItem.isVolume
            }

        }

    private var upDisplayTileJob: Coroutine<*>? = null

    override fun onCurrentListChanged() {
        super.onCurrentListChanged()
        callback.onListChanged()
    }

    fun clearDisplayTitle() {
        upDisplayTileJob?.cancel()
        displayTitleMap.clear()
    }

    fun upDisplayTitles(startIndex: Int) {
        upDisplayTileJob?.cancel()
        upDisplayTileJob = Coroutine.async(callback.scope) {
            val book = callback.book ?: return@async
            val replaceRules = ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
            val useReplace = AppConfig.tocUiUseReplace && book.getUseReplaceRule()
            val items = getItems()
            async {
                for (i in startIndex until items.size) {
                    val item = items[i]
                    if (displayTitleMap[item.title] == null) {
                        ensureActive()
                        val displayTitle = item.getDisplayTitle(replaceRules, useReplace)
                        ensureActive()
                        displayTitleMap[item.title] = displayTitle
                        withContext(Main) {
                            notifyItemChanged(i, true)
                        }
                    }
                }
            }.start()
            async {
                for (i in startIndex downTo 0) {
                    val item = items[i]
                    if (displayTitleMap[item.title] == null) {
                        ensureActive()
                        val displayTitle = item.getDisplayTitle(replaceRules, useReplace)
                        ensureActive()
                        displayTitleMap[item.title] = displayTitle
                        withContext(Main) {
                            notifyItemChanged(i, true)
                        }
                    }
                }
            }.start()
        }
    }

    private fun getDisplayTitle(chapter: BookChapter): String {
        return displayTitleMap[chapter.title] ?: chapter.title
    }

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = callback.durChapterIndex() == item.index
            val cached = callback.isLocalBook || cacheFileNames.contains(item.getFileName())
            if (payloads.isEmpty()) {
                if (isDur) {
                    tvChapterName.setTextColor(context.accentColor)
                } else {
                    tvChapterName.setTextColor(context.getCompatColor(R.color.primaryText))
                }
                tvChapterName.text = getDisplayTitle(item)
                if (item.isVolume) {
                    //卷名，如第一卷 突出显示
                    tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
                } else {
                    //普通章节 保持不变
                    tvChapterItem.background =
                        ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                }
                if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                    //卷名不显示tag(更新时间规则)
                    tvTag.text = item.tag
                    tvTag.visible()
                } else {
                    tvTag.gone()
                }
                upHasCache(binding, isDur, cached)
            } else {
                tvChapterName.text = getDisplayTitle(item)
                upHasCache(binding, isDur, cached)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.openChapter(it)
            }
        }
        holder.itemView.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let { item ->
                context.longToastOnUi(getDisplayTitle(item))
            }
            true
        }
    }

    private fun upHasCache(binding: ItemChapterListBinding, isDur: Boolean, cached: Boolean) =
        binding.apply {
            ivChecked.setImageResource(R.drawable.ic_outline_cloud_24)
            ivChecked.visible(!cached)
            if (isDur) {
                ivChecked.setImageResource(R.drawable.ic_check)
                ivChecked.visible()
            }
        }

    interface Callback {
        val scope: CoroutineScope
        val book: Book?
        val isLocalBook: Boolean
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
        fun onListChanged()
    }

}