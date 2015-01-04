package de.ph1b.audiobook.service;

import android.content.Context;
import android.content.Intent;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class Controls {

    private final Context c;

    public static final String CONTROL_INFORM_SPEED_CHANGED = "informSpeedChanged";

    public Controls(Context c) {
        this.c = c;
    }

    public void informSpeedChanged() {
        Intent i = new Intent(c, AudioPlayerService.class);
        i.setAction(CONTROL_INFORM_SPEED_CHANGED);
        c.startService(i);
    }
}
