package de.ph1b.audiobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import de.ph1b.audiobook.service.AudioPlayerService;


public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = "de.ph1b.audiobook.receiver.RemoteControlReceiver";
    public static final String KEYCODE = TAG + ".KEYCODE";

    @Override
    public void onReceive(Context context, Intent intent) {

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == (KeyEvent.ACTION_DOWN)) {
            int keyCode = event.getKeyCode();
            Intent i = new Intent(context, AudioPlayerService.class);
            i.putExtra(KEYCODE, keyCode);
            context.startService(i);
        }
    }
}
