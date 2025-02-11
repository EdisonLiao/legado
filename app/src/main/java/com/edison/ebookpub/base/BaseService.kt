package com.edison.ebookpub.base

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import com.edison.ebookpub.help.LifecycleHelp
import com.edison.ebookpub.help.coroutine.Coroutine
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class BaseService : Service(), CoroutineScope by MainScope() {

    fun <T> execute(
        scope: CoroutineScope = this,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ) = Coroutine.async(scope, context, start) { block() }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        LifecycleHelp.onServiceCreate(this)
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        cancel()
        LifecycleHelp.onServiceDestroy(this)
    }
}