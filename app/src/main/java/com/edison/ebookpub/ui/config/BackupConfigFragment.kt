package com.edison.ebookpub.ui.config

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.edison.ebookpub.R
import com.edison.ebookpub.constant.AppLog
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.help.AppWebDav
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.LocalConfig
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.help.storage.Backup
import com.edison.ebookpub.help.storage.BackupConfig
import com.edison.ebookpub.help.storage.ImportOldData
import com.edison.ebookpub.help.storage.Restore
import com.edison.ebookpub.lib.dialogs.alert
import com.edison.ebookpub.lib.permission.Permissions
import com.edison.ebookpub.lib.permission.PermissionsCompat
import com.edison.ebookpub.lib.prefs.fragment.PreferenceFragment
import com.edison.ebookpub.lib.theme.primaryColor
import com.edison.ebookpub.ui.document.HandleFileContract
import com.edison.ebookpub.ui.widget.dialog.TextDialog
import com.edison.ebookpub.utils.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import splitties.init.appCtx
import kotlin.collections.set

class BackupConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    MenuProvider {

    private val viewModel by activityViewModels<ConfigViewModel>()

    private val selectBackupPath = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
            } else {
                AppConfig.backupPath = uri.path
            }
        }
    }
    private val backupDir = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
                Coroutine.async {
                    Backup.backup(appCtx, uri.toString())
                }.onSuccess {
                    appCtx.toastOnUi(R.string.backup_success)
                }.onError {
                    AppLog.put("备份出错\n${it.localizedMessage}", it)
                    appCtx.toastOnUi(getString(R.string.backup_fail, it.localizedMessage))
                }
            } else {
                uri.path?.let { path ->
                    AppConfig.backupPath = path
                    Coroutine.async {
                        Backup.backup(appCtx, path)
                    }.onSuccess {
                        appCtx.toastOnUi(R.string.backup_success)
                    }.onError {
                        AppLog.put("备份出错\n${it.localizedMessage}", it)
                        appCtx.toastOnUi(getString(R.string.backup_fail, it.localizedMessage))
                    }
                }
            }
        }
    }
    private val restoreDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                AppConfig.backupPath = uri.toString()
                Coroutine.async {
                    Restore.restore(appCtx, uri.toString())
                }
            } else {
                uri.path?.let { path ->
                    AppConfig.backupPath = path
                    Coroutine.async {
                        Restore.restore(appCtx, path)
                    }
                }
            }
        }
    }
    private val restoreOld = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ImportOldData.importUri(appCtx, uri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_backup)
        findPreference<EditTextPreference>(PreferKey.webDavPassword)?.let {
            it.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }
        }
        upPreferenceSummary(PreferKey.webDavUrl, getPrefString(PreferKey.webDavUrl))
        upPreferenceSummary(PreferKey.webDavAccount, getPrefString(PreferKey.webDavAccount))
        upPreferenceSummary(PreferKey.webDavPassword, getPrefString(PreferKey.webDavPassword))
        upPreferenceSummary(PreferKey.webDavDir, AppConfig.webDavDir)
        upPreferenceSummary(PreferKey.backupPath, getPrefString(PreferKey.backupPath))
        findPreference<com.edison.ebookpub.lib.prefs.Preference>("web_dav_restore")
            ?.onLongClick { restoreDir.launch(); true }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.backup_restore)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        listView.setEdgeEffectColor(primaryColor)
        activity?.addMenuProvider(this, viewLifecycleOwner)
        if (!LocalConfig.backupHelpVersionIsLast) {
            showHelp()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.backup_restore, menu)
        menu.applyTint(requireContext())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_help -> {
                showHelp()
                return true
            }
        }
        return false
    }

    private fun showHelp() {
        val text = String(requireContext().assets.open("help/webDavHelp.md").readBytes())
        showDialogFragment(TextDialog(text, TextDialog.Mode.MD))
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.backupPath -> upPreferenceSummary(key, getPrefString(key))
            PreferKey.webDavUrl,
            PreferKey.webDavAccount,
            PreferKey.webDavPassword,
            PreferKey.webDavDir -> listView.post {
                upPreferenceSummary(key, getPrefString(key))
                viewModel.upWebDavConfig()
            }
        }
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.webDavUrl ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_url_s)
                } else {
                    preference.summary = value.toString()
                }
            PreferKey.webDavAccount ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_account_s)
                } else {
                    preference.summary = value.toString()
                }
            PreferKey.webDavPassword ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_pw_s)
                } else {
                    preference.summary = "*".repeat(value.toString().length)
                }
            PreferKey.webDavDir -> preference.summary = when (value) {
                null -> "legado"
                else -> value
            }
            else -> {
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(value)
                    // Set the summary to reflect the new value.
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    preference.summary = value
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.backupPath -> selectBackupPath.launch()
            PreferKey.restoreIgnore -> backupIgnore()
            "web_dav_backup" -> backup()
            "web_dav_restore" -> restore()
            "import_old" -> restoreOld.launch()
        }
        return super.onPreferenceTreeClick(preference)
    }

    /**
     * 备份忽略设置
     */
    private fun backupIgnore() {
        val checkedItems = BooleanArray(BackupConfig.ignoreKeys.size) {
            BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[it]] ?: false
        }
        alert(R.string.restore_ignore) {
            multiChoiceItems(BackupConfig.ignoreTitle, checkedItems) { _, which, isChecked ->
                BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[which]] = isChecked
            }
            onDismiss {
                BackupConfig.saveIgnoreConfig()
            }
        }
    }


    fun backup() {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            backupDir.launch()
        } else {
            if (backupPath.isContentScheme()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc?.canWrite() == true) {
                    Coroutine.async {
                        Backup.backup(requireContext(), backupPath)
                    }.onSuccess {
                        appCtx.toastOnUi(R.string.backup_success)
                    }.onError {
                        AppLog.put("备份出错\n${it.localizedMessage}", it)
                        appCtx.toastOnUi(
                            appCtx.getString(
                                R.string.backup_fail,
                                it.localizedMessage
                            )
                        )
                    }
                } else {
                    backupDir.launch()
                }
            } else {
                backupUsePermission(backupPath)
            }
        }
    }

    private fun backupUsePermission(path: String) {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Backup.backup(requireContext(), path)
                }.onSuccess {
                    appCtx.toastOnUi(R.string.backup_success)
                }.onError {
                    AppLog.put("备份出错\n${it.localizedMessage}", it)
                    appCtx.toastOnUi(appCtx.getString(R.string.backup_fail, it.localizedMessage))
                }
            }
            .request()
    }

    fun restore() {
        Coroutine.async {
            AppWebDav.showRestoreDialog(requireContext())
        }.onError {
            alert {
                setTitle(R.string.restore)
                setMessage("WebDavError\n${it.localizedMessage}\n将从本地备份恢复。")
                okButton {
                    restoreFromLocal()
                }
                cancelButton()
            }
        }
    }

    private fun restoreFromLocal() {
        val backupPath = getPrefString(PreferKey.backupPath)
        if (backupPath?.isNotEmpty() == true) {
            if (backupPath.isContentScheme()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc?.canWrite() == true) {
                    lifecycleScope.launch {
                        Restore.restore(requireContext(), backupPath)
                    }
                } else {
                    restoreDir.launch()
                }
            } else {
                restoreUsePermission(backupPath)
            }
        } else {
            restoreDir.launch()
        }
    }

    private fun restoreUsePermission(path: String) {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Restore.restoreDatabase(path)
                    Restore.restoreConfig(path)
                }
            }
            .request()
    }

}