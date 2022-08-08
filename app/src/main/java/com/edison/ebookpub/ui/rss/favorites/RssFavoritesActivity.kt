package com.edison.ebookpub.ui.rss.favorites

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.edison.ebookpub.base.BaseActivity
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.RssStar
import com.edison.ebookpub.databinding.ActivityRssFavoritesBinding
import com.edison.ebookpub.ui.rss.read.ReadRssActivity
import com.edison.ebookpub.ui.widget.recycler.VerticalDivider
import com.edison.ebookpub.utils.startActivity
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(),
    RssFavoritesAdapter.CallBack {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { RssFavoritesAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(VerticalDivider(this))
            it.adapter = adapter
        }
    }

    private fun initData() {
        launch {
            appDb.rssStarDao.liveAll().conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}