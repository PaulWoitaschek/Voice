package de.ph1b.audiobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;

import de.ph1b.audiobook.service.AudioPlayerService;


public class RemoteControlReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        LocalBroadcastManager lBCM = LocalBroadcastManager.getInstance(context);

        if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int eventKeyCode = event.getKeyCode();
            if (KeyEvent.ACTION_DOWN == event.getAction()) {
                switch (eventKeyCode) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        lBCM.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PLAY_PAUSE));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        lBCM.sendBroadcast(new Intent(AudioPlayerService.CONTROL_FORWARD));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        lBCM.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PREVIOUS));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        lBCM.sendBroadcast(new Intent((AudioPlayerService.CONTROL_FAST_FORWARD)));
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        lBCM.sendBroadcast(new Intent((AudioPlayerService.CONTROL_REWIND)));
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
