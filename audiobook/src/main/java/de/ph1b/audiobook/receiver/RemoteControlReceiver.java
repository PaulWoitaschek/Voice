package de.ph1b.audiobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.utils.L;


public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = RemoteControlReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == (KeyEvent.ACTION_DOWN)) {
            final int keyCode = event.getKeyCode();
            L.d(TAG, "retrieved keycode: " + keyCode);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(context, AudioPlayerService.class);
                    i.setAction(Intent.ACTION_MEDIA_BUTTON);
                    i.putExtra(Intent.EXTRA_KEY_EVENT, keyCode);
                    context.startService(i);
                }
            }).start();
        }
    }
}
