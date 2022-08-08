package com.edison.ebookpub.ui.book.bookmark

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseDialogFragment
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Bookmark
import com.edison.ebookpub.databinding.DialogBookmarkBinding
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.utils.setLayout
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import com.edison.ebookpub.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkDialog() : BaseDialogFragment(R.layout.dialog_bookmark) {

    constructor(bookmark: Bookmark, editPos: Int = -1) : this() {
        arguments = Bundle().apply {
            putInt("editPos", editPos)
            putParcelable("bookmark", bookmark)
        }
    }

    private val binding by viewBinding(DialogBookmarkBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val arguments = arguments ?: let {
            dismiss()
            return
        }
        val bookmark = arguments.getParcelable<Bookmark>("bookmark")
        bookmark ?: let {
            dismiss()
            return
        }
        val editPos = arguments.getInt("editPos", -1)
        binding.tvFooterLeft.visible(editPos >= 0)
        binding.run {
            tvChapterName.text = bookmark.chapterName
            editBookText.setText(bookmark.bookText)
            editContent.setText(bookmark.content)
            tvCancel.setOnClickListener {
                dismiss()
            }
            tvOk.setOnClickListener {
                bookmark.bookText = editBookText.text?.toString() ?: ""
                bookmark.content = editContent.text?.toString() ?: ""
                launch {
                    withContext(IO) {
                        appDb.bookmarkDao.insert(bookmark)
                    }
                    getCallback()?.upBookmark(editPos, bookmark)
                    dismiss()
                }
            }
            tvFooterLeft.setOnClickListener {
                launch {
                    withContext(IO) {
                        appDb.bookmarkDao.delete(bookmark)
                    }
                    getCallback()?.deleteBookmark(editPos)
                    dismiss()
                }
            }
        }
    }

    private fun getCallback(): Callback? {
        return (parentFragment as? Callback)
            ?: activity as? Callback
    }

    interface Callback {

        fun upBookmark(pos: Int, bookmark: Bookmark)

        fun deleteBookmark(pos: Int)

    }

}