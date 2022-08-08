@file:Suppress("DEPRECATION")

package com.edison.ebookpub.ui.main

import android.os.Bundle
import android.text.format.DateUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.edison.ebookpub.BuildConfig
import com.edison.ebookpub.R
import com.edison.ebookpub.base.VMBaseActivity
import com.edison.ebookpub.constant.AppConst.appInfo
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.databinding.ActivityMainBinding
import com.edison.ebookpub.help.AppWebDav
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.LocalConfig
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.help.storage.Backup
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.theme.elevation
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.service.BaseReadAloudService
import com.edison.ebookpub.ui.main.bookshelf.BaseBookshelfFragment
import com.edison.ebookpub.ui.main.bookshelf.style1.BookshelfFragment1
import com.edison.ebookpub.ui.main.bookshelf.style2.BookshelfFragment2
import com.edison.ebookpub.ui.main.explore.ExploreFragment
import com.edison.ebookpub.ui.main.my.MyFragment
import com.edison.ebookpub.ui.main.rss.RssFragment
import com.edison.ebookpub.ui.widget.dialog.TextDialog
import com.edison.ebookpub.utils.observeEvent
import com.edison.ebookpub.utils.setEdgeEffectColor
import com.edison.ebookpub.utils.showDialogFragment
import com.edison.ebookpub.utils.toastOnUi
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主界面
 */
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private val idBookshelf = 0
    private val idBookshelf1 = 11
    private val idBookshelf2 = 12
    private val idExplore = 1
    private val idRss = 2
    private val idMy = 3
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()
    private var bottomMenuCount = 4
    private val realPositions = arrayOf(idBookshelf, idExplore, idRss, idMy)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        upBottomMenu()
        binding.run {
            viewPagerMain.setEdgeEffectColor(primaryColor)
            viewPagerMain.offscreenPageLimit = 3
            viewPagerMain.adapter = TabFragmentPageAdapter(supportFragmentManager)
            viewPagerMain.addOnPageChangeListener(PageChangeCallback())
            bottomNavigationView.elevation = elevation
            bottomNavigationView.setOnNavigationItemSelectedListener(this@MainActivity)
            bottomNavigationView.setOnNavigationItemReselectedListener(this@MainActivity)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        upVersion()
        privacyPolicy()
        //自动更新书籍
        val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
        if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
            binding.viewPagerMain.postDelayed(1000) {
                viewModel.upAllBookToc()
            }
        }
        binding.viewPagerMain.postDelayed(3000) {
            viewModel.postLoad()
        }
        syncAlert()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = binding.run {
        when (item.itemId) {
            R.id.menu_bookshelf ->
                viewPagerMain.setCurrentItem(0, false)
            R.id.menu_discovery ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idExplore), false)
            R.id.menu_rss ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idRss), false)
            R.id.menu_my_config ->
                viewPagerMain.setCurrentItem(realPositions.indexOf(idMy), false)
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.gotoTop()
                }
            }
            R.id.menu_discovery -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[1] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }

    private fun upVersion() {
        if (LocalConfig.versionCode != appInfo.versionCode) {
            LocalConfig.versionCode = appInfo.versionCode
            if (LocalConfig.isFirstOpenApp) {
                val help = String(assets.open("help/appHelp.md").readBytes())
                showDialogFragment(TextDialog(help, TextDialog.Mode.MD))
            } else if (!BuildConfig.DEBUG) {
                val log = String(assets.open("updateLog.md").readBytes())
                showDialogFragment(TextDialog(log, TextDialog.Mode.MD))
            }
            viewModel.upVersion()
        }
    }

    /**
     * 同步提示
     */
    private fun syncAlert() = launch {
        val lastBackupFile = withContext(IO) { AppWebDav.lastBackUp().getOrNull() }
            ?: return@launch
        if (lastBackupFile.lastModify - LocalConfig.lastBackup > DateUtils.MINUTE_IN_MILLIS) {
            LocalConfig.lastBackup = lastBackupFile.lastModify
            alert("恢复", "webDav书源比本地新,是否恢复") {
                cancelButton()
                okButton {
                    viewModel.restoreWebDav(lastBackupFile.displayName)
                }
            }
        }
    }

    /**
     * 用户隐私与协议
     */
    private fun privacyPolicy() {
        if (LocalConfig.privacyPolicyOk) return
        val privacyPolicy = String(assets.open("privacyPolicy.md").readBytes())
        alert("用户隐私与协议", privacyPolicy) {
            noButton()
            yesButton {
                LocalConfig.privacyPolicyOk = true
            }
            onCancelled {
                finish()
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled) {
                    if (pagePosition != 0) {
                        binding.viewPagerMain.currentItem = 0
                        return true
                    }
                    (fragmentMap[getFragmentId(0)] as? BookshelfFragment2)?.let {
                        if (it.back()) {
                            return true
                        }
                    }
                    if (System.currentTimeMillis() - exitTime > 2000) {
                        toastOnUi(R.string.double_click_exit)
                        exitTime = System.currentTimeMillis()
                    } else {
                        if (BaseReadAloudService.pause) {
                            finish()
                        } else {
                            moveTaskToBack(true)
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (AppConfig.autoRefreshBook) {
            outState.putBoolean("isAutoRefreshedBook", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async {
            BookHelp.clearInvalidCache()
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            binding.apply {
                upBottomMenu()
                viewPagerMain.adapter?.notifyDataSetChanged()
                if (it) {
                    viewPagerMain.setCurrentItem(bottomMenuCount - 1, false)
                }
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private fun upBottomMenu() {
        val showDiscovery = AppConfig.showDiscovery
        val showRss = AppConfig.showRSS
        binding.bottomNavigationView.menu.let { menu ->
            menu.findItem(R.id.menu_discovery).isVisible = showDiscovery
            menu.findItem(R.id.menu_rss).isVisible = showRss
        }
        var index = 0
        if (showDiscovery) {
            index++
            realPositions[index] = idExplore
        }
        if (showRss) {
            index++
            realPositions[index] = idRss
        }
        index++
        realPositions[index] = idMy
        bottomMenuCount = index + 1
    }

    private fun getFragmentId(position: Int): Int {
        val id = realPositions[position]
        if (id == idBookshelf) {
            return if (AppConfig.bookGroupStyle == 1) idBookshelf2 else idBookshelf1
        }
        return id
    }

    private inner class PageChangeCallback : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            pagePosition = position
            binding.bottomNavigationView.menu
                .getItem(realPositions[position]).isChecked = true
        }

    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return getFragmentId(position)
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                idBookshelf1 -> BookshelfFragment1()
                idBookshelf2 -> BookshelfFragment2()
                idExplore -> ExploreFragment()
                idRss -> RssFragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return bottomMenuCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }

}