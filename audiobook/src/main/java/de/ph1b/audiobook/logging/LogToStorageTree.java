package de.ph1b.audiobook.logging;

import android.support.annotation.NonNull;

import de.ph1b.audiobook.persistence.LogStorage;

/**
 * Timber tree that delegates log messages to the log storage.
 *
 * @author Paul Woitschek
 */
public class LogToStorageTree extends FormattedTree {

    private final LogStorage logStorage;

    public LogToStorageTree(@NonNull LogStorage logStorage) {
        this.logStorage = logStorage;
    }

    @Override
    public void onLogGathered(@NonNull String message) {
        logStorage.put(message);
    }
}
