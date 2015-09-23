package de.ph1b.audiobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import de.ph1b.audiobook.service.BookReaderService;

public class RemoteControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        Intent i = new Intent(context, BookReaderService.class);
        i.setAction(Intent.ACTION_MEDIA_BUTTON);
        i.putExtra(Intent.EXTRA_KEY_EVENT, event);
        context.startService(i);
    }
}
