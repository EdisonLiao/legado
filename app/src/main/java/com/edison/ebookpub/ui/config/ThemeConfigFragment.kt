package com.edison.ebookpub.ui.config

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.core.view.MenuProvider
import androidx.preference.Preference
import com.edison.ebookpub.R
import com.edison.ebookpub.base.AppContextWrapper
import com.edison.ebookpub.constant.AppConst
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.databinding.DialogEditTextBinding
import com.edison.ebookpub.databinding.DialogImageBlurringBinding
import com.edison.ebookpub.help.LauncherIconHelp
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.ThemeConfig
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.dialogs.selector
import com.edison.ebookpub.lib.prefs.ColorPreference
import com.edison.ebookpub.lib.prefs.fragment.PreferenceFragment
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.ui.widget.number.NumberPickerDialog
import com.edison.ebookpub.ui.widget.seekbar.SeekBarChangeListener
import com.edison.ebookpub.utils.*
import java.io.FileOutputStream


@Suppress("SameParameterValue")
class ThemeConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    MenuProvider {

    private val requestCodeBgLight = 121
    private val requestCodeBgDark = 122
    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                requestCodeBgLight -> setBgFromUri(uri, PreferKey.bgImage) {
                    upTheme(false)
                }
                requestCodeBgDark -> setBgFromUri(uri, PreferKey.bgImageN) {
                    upTheme(true)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
        if (Build.VERSION.SDK_INT < 26) {
            preferenceScreen.removePreferenceRecursively(PreferKey.launcherIcon)
        }
        if (!AppConfig.isGooglePlay) {
            preferenceScreen.removePreferenceRecursively("welcomeStyle")
        }
        upPreferenceSummary(PreferKey.bgImage, getPrefString(PreferKey.bgImage))
        upPreferenceSummary(PreferKey.bgImageN, getPrefString(PreferKey.bgImageN))
        upPreferenceSummary(PreferKey.barElevation, AppConfig.elevation.toString())
        upPreferenceSummary(PreferKey.fontScale)
        findPreference<ColorPreference>(PreferKey.cBackground)?.let {
            it.onSaveColor = { color ->
                if (!ColorUtils.isColorLight(color)) {
                    toastOnUi(R.string.day_background_too_dark)
                    true
                } else {
                    false
                }
            }
        }
        findPreference<ColorPreference>(PreferKey.cNBackground)?.let {
            it.onSaveColor = { color ->
                if (ColorUtils.isColorLight(color)) {
                    toastOnUi(R.string.night_background_too_light)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.theme_setting)
        listView.setEdgeEffectColor(primaryColor)
        activity?.addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.theme_config, menu)
        menu.applyTint(requireContext())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_theme_mode -> {
                AppConfig.isNightTheme = !AppConfig.isNightTheme
                ThemeConfig.applyDayNight(requireContext())
                return true
            }
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.launcherIcon -> LauncherIconHelp.changeIcon(getPrefString(key))
            PreferKey.transparentStatusBar -> recreateActivities()
            PreferKey.immNavigationBar -> recreateActivities()
            PreferKey.cPrimary,
            PreferKey.cAccent,
            PreferKey.cBackground,
            PreferKey.cBBackground -> {
                upTheme(false)
            }
            PreferKey.cNPrimary,
            PreferKey.cNAccent,
            PreferKey.cNBackground,
            PreferKey.cNBBackground -> {
                upTheme(true)
            }
            PreferKey.bgImage,
            PreferKey.bgImageN -> {
                upPreferenceSummary(key, getPrefString(key))
            }
        }

    }

    @SuppressLint("PrivateResource")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (val key = preference.key) {
            PreferKey.barElevation -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.bar_elevation))
                .setMaxValue(32)
                .setMinValue(0)
                .setValue(AppConfig.elevation)
                .setCustomButton((R.string.btn_default_s)) {
                    AppConfig.elevation = AppConst.sysElevation
                    recreateActivities()
                }
                .show {
                    AppConfig.elevation = it
                    recreateActivities()
                }
            PreferKey.fontScale -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.font_scale))
                .setMaxValue(16)
                .setMinValue(8)
                .setValue(10)
                .setCustomButton((R.string.btn_default_s)) {
                    putPrefInt(PreferKey.fontScale, 0)
                    recreateActivities()
                }
                .show {
                    putPrefInt(PreferKey.fontScale, it)
                    recreateActivities()
                }
            PreferKey.bgImage -> selectBgAction(false)
            PreferKey.bgImageN -> selectBgAction(true)
            "themeList" -> ThemeListDialog().show(childFragmentManager, "themeList")
            "saveDayTheme",
            "saveNightTheme" -> alertSaveTheme(key)
            "coverConfig" -> (activity as? ConfigActivity)
                ?.replaceFragment<CoverConfigFragment>(ConfigTag.COVER_CONFIG)
            "welcomeStyle" -> (activity as? ConfigActivity)
                ?.replaceFragment<WelcomeConfigFragment>(ConfigTag.WELCOME_CONFIG)
        }
        return super.onPreferenceTreeClick(preference)
    }

    @SuppressLint("InflateParams")
    private fun alertSaveTheme(key: String) {
        alert(R.string.theme_name) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "name"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let { themeName ->
                    when (key) {
                        "saveDayTheme" -> {
                            ThemeConfig.saveDayTheme(requireContext(), themeName)
                        }
                        "saveNightTheme" -> {
                            ThemeConfig.saveNightTheme(requireContext(), themeName)
                        }
                    }
                }
            }
            noButton()
        }
    }

    private fun selectBgAction(isNight: Boolean) {
        val bgKey = if (isNight) PreferKey.bgImageN else PreferKey.bgImage
        val blurringKey = if (isNight) PreferKey.bgImageNBlurring else PreferKey.bgImageBlurring
        val actions = arrayListOf(
            getString(R.string.background_image_blurring),
            getString(R.string.select_image)
        )
        if (!getPrefString(bgKey).isNullOrEmpty()) {
            actions.add(getString(R.string.delete))
        }
        context?.selector(items = actions) { _, i ->
            when (i) {
                0 -> alertImageBlurring(blurringKey) {
                    upTheme(isNight)
                }
                1 -> {
                    if (isNight) {
                        selectImage.launch(requestCodeBgDark)
                    } else {
                        selectImage.launch(requestCodeBgLight)
                    }
                }
                2 -> {
                    removePref(bgKey)
                    upTheme(isNight)
                }
            }
        }
    }

    private fun alertImageBlurring(preferKey: String, success: () -> Unit) {
        alert(R.string.background_image_blurring) {
            val alertBinding = DialogImageBlurringBinding.inflate(layoutInflater).apply {
                getPrefInt(preferKey, 0).let {
                    seekBar.progress = it
                    textViewValue.text = it.toString()
                }
                seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        textViewValue.text = progress.toString()
                    }
                })
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.seekBar.progress.let {
                    putPrefInt(preferKey, it)
                    success.invoke()
                }
            }
            noButton()
        }
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (AppConfig.isNightTheme == isNightTheme) {
            listView.post {
                ThemeConfig.applyTheme(requireContext())
                recreateActivities()
            }
        }
    }

    private fun recreateActivities() {
        postEvent(EventBus.RECREATE, "")
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String? = null) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.barElevation -> preference.summary =
                getString(R.string.bar_elevation_s, value)
            PreferKey.fontScale -> {
                val fontScale = AppContextWrapper.getFontScale(requireContext())
                preference.summary = getString(R.string.font_scale_summary, fontScale)
            }
            PreferKey.bgImage,
            PreferKey.bgImageN -> preference.summary = if (value.isNullOrBlank()) {
                getString(R.string.select_image)
            } else {
                value
            }
            else -> preference.summary = value
        }
    }

    private fun setBgFromUri(uri: Uri, preferenceKey: String, success: () -> Unit) {
        readUri(uri) { fileDoc, inputStream ->
            var file = requireContext().externalFiles
            file = FileUtils.createFileIfNotExist(file, preferenceKey, fileDoc.name)
            FileOutputStream(file).use {
                inputStream.copyTo(it)
            }
            putPrefString(preferenceKey, file.absolutePath)
            success()
        }
    }

}