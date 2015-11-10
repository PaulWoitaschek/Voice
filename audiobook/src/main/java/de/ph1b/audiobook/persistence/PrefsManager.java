package de.ph1b.audiobook.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.interfaces.ForApplication;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.Communication;

/**
 * Preference manager, managing the setting and getting of {@link SharedPreferences}
 *
 * @author Paul Woitaschek
 */
@Singleton
public class PrefsManager {

    private static final String PREF_KEY_CURRENT_BOOK = "currentBook";
    private static final String PREF_KEY_COLLECTION_FOLDERS = "folders";
    private static final String PREF_KEY_SINGLE_BOOK_FOLDERS = "singleBookFolders";
    private static final String PREF_KEY_DISPLAY_MODE = "displayMode";
    private final String PREF_KEY_RESUME_ON_REPLUG;
    private final String PREF_KEY_THEME;
    private final Communication communication;
    private final SharedPreferences sp;
    private final String PREF_KEY_BOOKMARK_ON_SLEEP;
    private final String PREF_KEY_SEEK_TIME;
    private final String PREF_KEY_SLEEP_TIME;
    private final String PREF_KEY_PAUSE_ON_CAN_DUCK;
    private final String PREF_KEY_AUTO_REWIND;

    @Inject
    public PrefsManager(@NonNull @ForApplication Context c, @NonNull Communication communication) {
        PreferenceManager.setDefaultValues(c, R.xml.preferences, false);
        this.communication = communication;
        sp = PreferenceManager.getDefaultSharedPreferences(c);

        PREF_KEY_THEME = c.getString(R.string.pref_key_theme);
        PREF_KEY_SLEEP_TIME = c.getString(R.string.pref_key_sleep_time);
        PREF_KEY_RESUME_ON_REPLUG = c.getString(R.string.pref_key_resume_on_replug);
        PREF_KEY_SEEK_TIME = c.getString(R.string.pref_key_seek_time);
        PREF_KEY_BOOKMARK_ON_SLEEP = c.getString(R.string.pref_key_bookmark_on_sleep);
        PREF_KEY_AUTO_REWIND = c.getString(R.string.pref_key_auto_rewind);
        PREF_KEY_PAUSE_ON_CAN_DUCK = c.getString(R.string.pref_key_pause_on_can_duck);
    }

    public synchronized ThemeUtil.Theme getTheme() {
        String value = sp.getString(PREF_KEY_THEME, null);
        if (value == null) {
            return ThemeUtil.Theme.LIGHT;
        } else {
            return ThemeUtil.Theme.valueOf(value);
        }
    }

    public synchronized void setTheme(ThemeUtil.Theme theme) {
        sp.edit().putString(PREF_KEY_THEME, theme.name()).apply();
    }

    /**
     * @return the id of the current book, or {@link de.ph1b.audiobook.model.Book#ID_UNKNOWN} if
     * there is none.
     */
    public synchronized long getCurrentBookId() {
        return sp.getLong(PREF_KEY_CURRENT_BOOK, Book.ID_UNKNOWN);
    }

    /**
     * Sets the current bookId and calls {@link Communication#sendCurrentBookChanged(long)}
     *
     * @param bookId the book Id to set
     */
    public synchronized void setCurrentBookIdAndInform(long bookId) {
        long oldId = getCurrentBookId();
        sp.edit().putLong(PREF_KEY_CURRENT_BOOK, bookId)
                .apply();
        communication.sendCurrentBookChanged(oldId);
    }

    /**
     * @return All book paths that are set as {@link de.ph1b.audiobook.model.Book.Type#COLLECTION_FOLDER}
     * or {@link de.ph1b.audiobook.model.Book.Type#COLLECTION_FILE}
     */
    @NonNull
    public synchronized List<String> getCollectionFolders() {
        Set<String> set = sp.getStringSet(PREF_KEY_COLLECTION_FOLDERS, new HashSet<>(10));
        return new ArrayList<>(set);
    }

    /**
     * Sets the folders that represent a collection.{@link de.ph1b.audiobook.model.Book.Type#COLLECTION_FOLDER}
     * or {@link de.ph1b.audiobook.model.Book.Type#COLLECTION_FILE}
     *
     * @param folders the collection folders
     * @see PrefsManager#getCollectionFolders()
     */
    public synchronized void setCollectionFolders(@NonNull List<String> folders) {
        Set<String> set = new HashSet<>(folders.size());
        set.addAll(folders);
        sp.edit().putStringSet(PREF_KEY_COLLECTION_FOLDERS, set)
                .apply();
    }

    /**
     * Like {@link PrefsManager#getCollectionFolders()} but with
     * {@link de.ph1b.audiobook.model.Book.Type#SINGLE_FILE} or
     * {@link de.ph1b.audiobook.model.Book.Type#SINGLE_FOLDER}.
     *
     * @return the single book folders
     */
    @NonNull
    public synchronized List<String> getSingleBookFolders() {
        Set<String> set = sp.getStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, new HashSet<>(10));
        return new ArrayList<>(set);
    }

    /**
     * Like {@link PrefsManager#setCollectionFolders(List)} ()} but with
     * {@link de.ph1b.audiobook.model.Book.Type#SINGLE_FILE} or
     * {@link de.ph1b.audiobook.model.Book.Type#SINGLE_FOLDER}.
     *
     * @param folders the single book folders
     */
    public synchronized void setSingleBookFolders(@NonNull List<String> folders) {
        Set<String> set = new HashSet<>(folders.size());
        set.addAll(folders);
        sp.edit().putStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, set)
                .apply();
    }

    /**
     * Returns the time to sleep after which the player should pause the book when sleep timer has
     * been activated
     *
     * @return the time to sleep
     */
    public synchronized int getSleepTime() {
        return sp.getInt(PREF_KEY_SLEEP_TIME, 20);
    }

    /**
     * Returns the time after which the sleep timer should pause the book.
     *
     * @param time sleep time in Minutes
     */
    public synchronized void setSleepTime(int time) {
        sp.edit().putInt(PREF_KEY_SLEEP_TIME, time)
                .apply();
    }

    /**
     * Returns the time to seek when pressing a skip button.
     *
     * @return the time to seek
     */
    public synchronized int getSeekTime() {
        return sp.getInt(PREF_KEY_SEEK_TIME, 20);
    }

    /**
     * Sets the time to seek when pressing a skip button
     *
     * @param time the time to seek
     */
    public synchronized void setSeekTime(int time) {
        sp.edit().putInt(PREF_KEY_SEEK_TIME, time)
                .apply();
    }

    /**
     * @return true if the player should resume after the headset has been replugged. (If previously
     * paused by unplugging).
     */
    public synchronized boolean resumeOnReplug() {
        return sp.getBoolean(PREF_KEY_RESUME_ON_REPLUG, true);
    }

    /**
     * @return true if should pause the player on a temporary interruption.
     */
    public synchronized boolean pauseOnTempFocusLoss() {
        return sp.getBoolean(PREF_KEY_PAUSE_ON_CAN_DUCK, false);
    }

    /**
     * Returns the amount to rewind after the player was paused manually.
     *
     * @return the rewind amount
     */
    public synchronized int getAutoRewindAmount() {
        return sp.getInt(PREF_KEY_AUTO_REWIND, 2);
    }

    /**
     * Sets the time the player should rewind on manual pausing.
     *
     * @param autoRewindAmount the amount to auto rewind
     */
    public synchronized void setAutoRewindAmount(int autoRewindAmount) {
        sp.edit().putInt(PREF_KEY_AUTO_REWIND, autoRewindAmount)
                .apply();
    }

    /**
     * @return true if a {@link de.ph1b.audiobook.model.Bookmark} should be set each time the sleep
     * timer is called
     */
    public synchronized boolean setBookmarkOnSleepTimer() {
        return sp.getBoolean(PREF_KEY_BOOKMARK_ON_SLEEP, false);
    }

    /**
     * @return the display mode that has been set or the default.
     */
    public synchronized BookShelfFragment.DisplayMode getDisplayMode() {
        return BookShelfFragment.DisplayMode.valueOf(sp.getString(PREF_KEY_DISPLAY_MODE, BookShelfFragment.DisplayMode.GRID.name()));
    }

    /**
     * Sets the display mode.
     *
     * @param displayMode the mode to set
     */
    public synchronized void setDisplayMode(BookShelfFragment.DisplayMode displayMode) {
        sp.edit().putString(PREF_KEY_DISPLAY_MODE, displayMode.name()).apply();
    }
}
