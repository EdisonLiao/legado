package com.edison.ebookpub.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.edison.ebookpub.constant.EventBus
import com.edison.ebookpub.utils.postEvent


class TimeBatteryReceiver : BroadcastReceiver() {

    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_TICK -> {
                postEvent(EventBus.TIME_CHANGED, "")
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                postEvent(EventBus.BATTERY_CHANGED, level)
            }
        }
    }

}