package de.ph1b.audiobook.playback.events

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.ph1b.audiobook.playback.BookReaderService

/**
 * Forwards intents to [BookReaderService]
 *
 * @author Paul Woitaschek
 */
class MediaEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            context.startService(Intent(intent).apply {
                component = ComponentName(context, BookReaderService::class.java)
            })
        }
    }
}