package com.edison.ebookpub.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseFragment
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.databinding.FragmentMyConfigBinding
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.ThemeConfig
import com.edison.ebookpub.lib.dialogs.selector
import com.edison.ebookpub.lib.prefs.NameListPreference
import com.edison.ebookpub.lib.prefs.PreferenceCategory
import com.edison.ebookpub.lib.prefs.SwitchPreference
import com.edison.ebookpub.lib.prefs.fragment.PreferenceFragment
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.service.WebService
import com.edison.ebookpub.ui.about.AboutActivity
import com.edison.ebookpub.ui.about.DonateActivity
import com.edison.ebookpub.ui.about.ReadRecordActivity
import com.edison.ebookpub.ui.book.bookmark.AllBookmarkActivity
import com.edison.ebookpub.ui.book.source.manage.BookSourceActivity
import com.edison.ebookpub.ui.config.ConfigActivity
import com.edison.ebookpub.ui.config.ConfigTag
import com.edison.ebookpub.ui.replace.ReplaceRuleActivity
import com.edison.ebookpub.ui.widget.dialog.TextDialog
import com.edison.ebookpub.utils.*
import com.edison.ebookpub.utils.viewbindingdelegate.viewBinding

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> {
                val text = String(requireContext().assets.open("help/appHelp.md").readBytes())
                showDialogFragment(TextDialog(text, TextDialog.Mode.MD))
            }
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
            if (AppConfig.isGooglePlay) {
                findPreference<PreferenceCategory>("aboutCategory")
                    ?.removePreferenceRecursively("donate")
            }
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) {
                    return@onLongClick false
                }
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }
                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }
                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }
                "readRecord" -> startActivity<ReadRecordActivity>()
                "donate" -> startActivity<DonateActivity>()
                "about" -> startActivity<AboutActivity>()
            }
            return super.onPreferenceTreeClick(preference)
        }


    }
}