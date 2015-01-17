package de.ph1b.audiobook.service;

import android.content.Context;
import android.content.Intent;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class Controls {

    public static final String CONTROL_INFORM_SPEED_CHANGED = "informSpeedChanged";

    public static final String CONTROL_CHANGE_BOOK_POSITION = "control";
    public static final String CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_ID = "controlMediaId";
    public static final String CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION = "controlMediaPosition";
    private final Context c;

    public Controls(Context c) {
        this.c = c;
    }

    public void informSpeedChanged() {
        Intent i = new Intent(c, AudioPlayerService.class);
        i.setAction(CONTROL_INFORM_SPEED_CHANGED);
        c.startService(i);
    }

    public void changeBookPosition(long mediaId, int position) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(CONTROL_CHANGE_BOOK_POSITION);
        intent.putExtra(CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_ID, mediaId);
        intent.putExtra(CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION, position);
        c.startService(intent);
    }
}
