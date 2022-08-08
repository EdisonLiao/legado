package com.edison.ebookpub.ui.config

import android.app.Application
import com.edison.ebookpub.R
import com.edison.ebookpub.base.BaseViewModel
import com.edison.ebookpub.help.AppWebDav
import com.edison.ebookpub.help.BookHelp
import com.edison.ebookpub.utils.FileUtils
import com.edison.ebookpub.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }


}