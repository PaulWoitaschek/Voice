package de.ph1b.audiobook.logging;

import android.support.annotation.NonNull;
import android.util.Log;

import timber.log.Timber;

/**
 * A Timber tree that formats messages based on their priority.
 *
 * @author Paul Woitaschek
 */
public abstract class FormattedTree extends Timber.DebugTree {
    /**
     * Maps Log priority to Strings
     *
     * @param priority priority
     * @return the mapped string or the priority as a string if no mapping could be made.
     */
    private static String priorityToPrefix(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return "V";
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            case Log.ASSERT:
                return "A";
            default:
                return String.valueOf(priority);
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        onLogGathered(priorityToPrefix(priority) + "/[" + tag + "]\t" + message + "\n");
    }

    public abstract void onLogGathered(@NonNull String message);
}
