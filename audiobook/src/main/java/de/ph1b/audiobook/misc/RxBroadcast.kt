package de.ph1b.audiobook.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import rx.Observable
import rx.subscriptions.Subscriptions

/**
 * Wraps a broadcast receiver in an observable that registers and unregisters based on the subscription.
 *
 * @author Paul Woitaschek
 */
object RxBroadcast {
    fun register(c: Context, filter: IntentFilter) = Observable.create<Intent?> {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (!it.isUnsubscribed) it.onNext(intent)
            }
        }
        c.registerReceiver(receiver, filter)
        it.add(Subscriptions.create { c.unregisterReceiver(receiver) })
    }
}