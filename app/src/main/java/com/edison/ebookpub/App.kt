package com.edison.ebookpub

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.multidex.MultiDexApplication
import com.github.liuyueyi.quick.transfer.ChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.jeremyliao.liveeventbus.LiveEventBus
import com.edison.ebookpub.base.AppContextWrapper
import com.edison.ebookpub.constant.AppConst.channelIdDownload
import com.edison.ebookpub.constant.AppConst.channelIdReadAloud
import com.edison.ebookpub.constant.AppConst.channelIdWeb
import com.edison.ebookpub.constant.PreferKey
import com.edison.ebookpub.data.appDb
import com.edison.ebookpub.help.*
import com.edison.ebookpub.help.config.AppConfig
import com.edison.ebookpub.help.config.ThemeConfig.applyDayNight
import com.edison.ebookpub.help.coroutine.Coroutine
import com.edison.ebookpub.help.http.cronet.CronetLoader
import com.edison.ebookpub.model.BookCover
import com.edison.ebookpub.utils.defaultSharedPreferences
import com.edison.ebookpub.utils.getPrefBoolean
import splitties.systemservices.notificationManager
import java.util.concurrent.TimeUnit

class App : MultiDexApplication() {

    private lateinit var oldConfig: Configuration

    override fun onCreate() {
        super.onCreate()
        oldConfig = Configuration(resources.configuration)
        CrashHandler(this)
        //预下载Cronet so
        CronetLoader.preDownload()
        createNotificationChannels()
        applyDayNight(this)
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        Coroutine.async {
            //初始化封面
            BookCover.toString()
            //清除过期数据
            appDb.cacheDao.clearDeadline(System.currentTimeMillis())
            if (getPrefBoolean(PreferKey.autoClearExpired, true)) {
                val clearTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                appDb.searchBookDao.clearExpired(clearTime)
            }
            RuleBigDataHelp.clearInvalid()
            BookHelp.clearInvalidCache()
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> ChineseUtils.preLoad(true, TransType.TRADITIONAL_TO_SIMPLE)
                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
            //同步阅读记录
            if (AppWebDav.syncBookProgress) {
                AppWebDav.downloadAllBookProgress()
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val diff = newConfig.diff(oldConfig)
        if ((diff and ActivityInfo.CONFIG_UI_MODE) != 0) {
            applyDayNight(this)
        }
        oldConfig = Configuration(newConfig)
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val downloadChannel = NotificationChannel(
            channelIdDownload,
            getString(R.string.action_download),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(R.string.web_service),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }

        //向notification manager 提交channel
        notificationManager.createNotificationChannels(
            listOf(
                downloadChannel,
                readAloudChannel,
                webChannel
            )
        )
    }

}
