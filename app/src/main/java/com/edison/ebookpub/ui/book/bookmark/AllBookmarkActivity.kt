package com.edison.ebookpub.ui.book.bookmark

import android.os.Bundle
import androidx.activity.viewModels
import com.edison.ebookpub.base.VMBaseActivity
import com.edison.ebookpub.data.entities.Bookmark
import com.edison.ebookpub.databinding.ActivityAllBookmarkBinding
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

class AllBookmarkActivity : VMBaseActivity<ActivityAllBookmarkBinding, AllBookmarkViewModel>(),
    BookmarkAdapter.Callback,
    BookmarkDialog.Callback {

    override val viewModel by viewModels<AllBookmarkViewModel>()
    override val binding by viewBinding(ActivityAllBookmarkBinding::inflate)
    private val adapter by lazy {
        BookmarkAdapter(this, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData {
            adapter.setItems(it)
        }
    }

    private fun initView() {
        binding.recyclerView.addItemDecoration(BookmarkDecoration(adapter))
        binding.recyclerView.adapter = adapter
    }

    override fun onItemClick(bookmark: Bookmark, position: Int) {
        showDialogFragment(BookmarkDialog(bookmark, position))
    }

    override fun upBookmark(pos: Int, bookmark: Bookmark) {
        adapter.setItem(pos, bookmark)
    }

    override fun deleteBookmark(pos: Int) {
        adapter.getItem(pos)?.let {
            viewModel.deleteBookmark(it)
        }
        adapter.removeItem(pos)
    }
}