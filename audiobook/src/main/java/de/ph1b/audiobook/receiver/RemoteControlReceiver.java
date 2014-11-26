package de.ph1b.audiobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.service.AudioPlayerService;


public class RemoteControlReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(final Context context, Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == (KeyEvent.ACTION_DOWN)) {
            final int keyCode = event.getKeyCode();
            if (BuildConfig.DEBUG) Log.d("rmcr", "retrieved keycode: " + keyCode);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(context, AudioPlayerService.class);
                    i.putExtra(Intent.EXTRA_KEY_EVENT, keyCode);
                    context.startService(i);
                }
            }).start();
        }
    }
}
