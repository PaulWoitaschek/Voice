package de.ph1b.audiobook.utils;

import android.app.Activity;
import android.support.v4.app.ActivityCompat;

/**
 * Simple class that helps postponing and releasing a transition.
 *
 * @author Paul Woitaschek
 */
public class TransitionPostponeHelper {

    private final Activity activity;
    private int postponeCount;

    /**
     * @param activity The activity to be postponed
     */
    public TransitionPostponeHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * @param postponeCount The amount of items after which the postpone should be released.
     */
    public synchronized void startPostponing(@SuppressWarnings("SameParameterValue") int postponeCount) {
        this.postponeCount = postponeCount;
        ActivityCompat.postponeEnterTransition(activity);
    }

    /**
     * Call this when an elements postpone is done. Once the {@link #postponeCount} reaches 0, the
     * postponed transition will start.
     */
    public synchronized void elementDone() {
        if (postponeCount == 0) {
            throw new IllegalStateException("Must not call this when the the postponing is already" +
                    " done");
        }
        postponeCount--;
        if (postponeCount == 0) {
            ActivityCompat.startPostponedEnterTransition(activity);
        }
    }
}
