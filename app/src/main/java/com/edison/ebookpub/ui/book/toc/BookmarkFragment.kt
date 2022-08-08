package com.edison.ebookpub.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseFragment
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.Bookmark
import com.edison.ebookpub.databinding.FragmentBookmarkBinding
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.ui.book.bookmark.BookmarkDialog
import com.edison.ebookpub.ui.widget.recycler.UpLinearLayoutManager
import com.edison.ebookpub.ui.widget.recycler.VerticalDivider
import com.edison.ebookpub.utils.setEdgeEffectColor
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    BookmarkDialog.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { BookmarkAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            durChapterIndex = it.durChapterIndex
            upBookmark(null)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    override fun upBookmark(searchKey: String?) {
        val book = viewModel.bookData.value ?: return
        launch {
            withContext(IO) {
                when {
                    searchKey.isNullOrBlank() -> appDb.bookmarkDao.getByBook(book.name, book.author)
                    else -> appDb.bookmarkDao.search(book.name, book.author, searchKey)
                }
            }.let {
                adapter.setItems(it)
                var scrollPos = 0
                withContext(Dispatchers.Default) {
                    adapter.getItems().forEachIndexed { index, bookmark ->
                        if (bookmark.chapterIndex >= durChapterIndex) {
                            return@withContext
                        }
                        scrollPos = index
                    }
                }
                mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            }
        }
    }


    override fun onClick(bookmark: Bookmark) {
        activity?.run {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("index", bookmark.chapterIndex)
                putExtra("chapterPos", bookmark.chapterPos)
            })
            finish()
        }
    }

    override fun onLongClick(bookmark: Bookmark, pos: Int) {
        showDialogFragment(BookmarkDialog(bookmark, pos))
    }

    override fun upBookmark(pos: Int, bookmark: Bookmark) {
        adapter.setItem(pos, bookmark)
    }

    override fun deleteBookmark(pos: Int) {
        adapter.removeItem(pos)
    }
}