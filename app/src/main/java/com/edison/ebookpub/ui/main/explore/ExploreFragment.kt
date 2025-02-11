package com.edison.ebookpub.ui.main.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edison.ebookpub.BuildConfig
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseFragment
import com.edison.ebookpub.constant.AppLog
import com.edison.ebookpub.constant.AppPattern
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.data.entities.BookSource
import com.edison.ebookpub.databinding.AdNativeTemplateBinding
import com.edison.ebookpub.databinding.FragmentExploreBinding
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.income.AdMgr
import com.edison.ebookpub.income.IAdmobRequestListener
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.lib.theme.primaryTextColor
import com.edison.ebookpub.ui.book.explore.ExploreShowActivity
import com.edison.ebookpub.ui.book.source.edit.BookSourceEditActivity
import com.edison.ebookpub.ui.widget.dialog.WaitDialog
import com.edison.ebookpub.utils.*
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import okhttp3.internal.wait

/**
 * 发现界面
 */
class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    ExploreAdapter.CallBack {

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreBinding::bind)
    private val adapter by lazy { ExploreAdapter(requireContext(), this) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val diffItemCallBack = ExploreDiffItemCallBack()
    private val groups = linkedSetOf<String>()
    private var exploreFlowJob: Job? = null
    private var groupsMenu: SubMenu? = null
    private var bottomNativeAd: NativeAd? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        upExploreData()
        initLocalBookSource()
        initBottomNativeAd()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        super.onCompatCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_explore, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus()
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                upExploreData(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.rvFind.setEdgeEffectColor(primaryColor)
        binding.rvFind.layoutManager = linearLayoutManager
        binding.rvFind.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.rvFind.scrollToPosition(0)
                }
            }
        })
    }

    private fun initGroupData() {
        launch {
            appDb.bookSourceDao.flowExploreGroup().conflate().collect {
                groups.clear()
                it.map { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                upGroupsMenu()
            }
        }
    }

    private fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        exploreFlowJob = launch {
            when {
                searchKey.isNullOrBlank() -> {
                    appDb.bookSourceDao.flowExplore()
                }
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.bookSourceDao.flowGroupExplore(key)
                }
                else -> {
                    appDb.bookSourceDao.flowExplore(searchKey)
                }
            }.catch {
                AppLog.put("发现界面更新数据出错", it)
            }.conflate().collect {
                binding.tvEmptyMsg.isGone = it.isNotEmpty() || searchView.query.isNotEmpty()
                adapter.setItems(it, diffItemCallBack)
            }
        }
    }

    private fun upGroupsMenu() = groupsMenu?.let { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    override val scope: CoroutineScope
        get() = lifecycleScope

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        if (item.groupId == R.id.menu_group_text) {
            searchView.setQuery("group:${item.title}", true)
        }
    }

    override fun refreshData() {
        upExploreData(searchView.query?.toString())
    }

    override fun scrollTo(pos: Int) {
        (binding.rvFind.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
    }

    override fun openExplore(sourceUrl: String, title: String, exploreUrl: String?) {
        if (exploreUrl.isNullOrBlank()) return
        startActivity<ExploreShowActivity> {
            putExtra("exploreName", title)
            putExtra("sourceUrl", sourceUrl)
            putExtra("exploreUrl", exploreUrl)
        }
    }

    override fun editSource(sourceUrl: String) {
        startActivity<BookSourceEditActivity> {
            putExtra("sourceUrl", sourceUrl)
        }
    }

    override fun toTop(source: BookSource) {
        viewModel.topSource(source)
    }

    private fun initLocalBookSource() {
        if (viewModel.getAllBookCount() <= 0) {
            viewModel.initLocalBookSource("local_booksource1.json")
            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.initLocalSourceDoneLiveData.observe(this, Observer { ret ->
                if (waitDialog.isShowing) {
                    waitDialog.dismiss()
                }
            })

            viewModel.errorLiveData.observe(this, Observer {
                if (waitDialog.isShowing) {
                    waitDialog.dismiss()
                }
            })
        }
    }

    private fun initBottomNativeAd(){
        var adId = ""
        if (BuildConfig.DEBUG){
            adId = "ca-app-pub-3940256099942544/2247696110"
        }

        AdMgr.requestNativeAd(requireContext(),adId,object : IAdmobRequestListener{
            override fun onLoadSuccess(adObject: NativeAd) {
                bottomNativeAd = adObject
                AdMgr.populateNativeAdView(bottomNativeAd!!,AdNativeTemplateBinding.inflate(layoutInflater),binding.flAd)
            }

            override fun onLoadFailed(errorCode: Int, errorMsg: String) {}

        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNativeAd?.destroy()
    }

    fun compressExplore() {
        if (!adapter.compressExplore()) {
            if (AppConfig.isEInkMode) {
                binding.rvFind.scrollToPosition(0)
            } else {
                binding.rvFind.smoothScrollToPosition(0)
            }
        }
    }

}