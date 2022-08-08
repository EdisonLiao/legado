package com.edison.ebookpub.ui.rss.source.debug

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.activity.viewModels
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseActivity
import com.edison.ebookpub.databinding.ActivitySourceDebugBinding
import com.edison.ebookpub.lib.theme.accentColor
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.ui.widget.dialog.TextDialog
import com.edison.ebookpub.utils.gone
import com.edison.ebookpub.utils.setEdgeEffectColor
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.toastOnUi
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch


class RssSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, RssSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<RssSourceDebugModel>()

    private val adapter by lazy { RssSourceDebugAdapter(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.observe { state, msg ->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.hide()
                }
            }
        }
        viewModel.initData(intent.getStringExtra("key")) {
            startSearch()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_list_src -> showDialogFragment(TextDialog(viewModel.listSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog(viewModel.contentSrc))
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        binding.titleBar.findViewById<SearchView>(R.id.search_view).gone()
    }

    private fun startSearch() {
        adapter.clearItems()
        viewModel.startDebug({
            binding.rotateLoading.show()
        }, {
            toastOnUi("未获取到源")
        })
    }
}