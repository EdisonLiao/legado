@file:Suppress("unused")

package com.edison.ebookpub.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/** This main looper cache avoids synchronization overhead when accessed repeatedly. */
@JvmField
val mainLooper: Looper = Looper.getMainLooper()

@JvmField
val mainThread: Thread = mainLooper.thread

val isMainThread: Boolean inline get() = mainThread === Thread.currentThread()

@PublishedApi
internal val currentThread: Any?
    inline get() = Thread.currentThread()

val mainHandler: Handler by lazy {
    if (SDK_INT >= 28) Handler.createAsync(mainLooper) else try {
        Handler::class.java.getDeclaredConstructor(
            Looper::class.java,
            Handler.Callback::class.java,
            Boolean::class.javaPrimitiveType // async
        ).newInstance(mainLooper, null, true)
    } catch (ignored: NoSuchMethodException) {
        // Hidden constructor absent. Fall back to non-async constructor.
        Handler(mainLooper)
    }
}

fun runOnUI(function: () -> Unit) {
    if (isMainThread) {
        function()
    } else {
        mainHandler.post(function)
    }
}

fun CoroutineScope.runOnIO(function: () -> Unit) {
    if (isMainThread) {
        launch(IO) {
            function()
        }
    } else {
        function()
    }
}