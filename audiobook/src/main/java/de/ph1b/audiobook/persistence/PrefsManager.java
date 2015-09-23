package de.ph1b.audiobook.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.utils.Communication;

/**
 * Preference manager, managing the setting and getting of {@link SharedPreferences}
 */
@ThreadSafe
public class PrefsManager {

    private static final String PREF_KEY_CURRENT_BOOK = "currentBook";
    private static final String PREF_KEY_COLLECTION_FOLDERS = "folders";
    private static final String PREF_KEY_SINGLE_BOOK_FOLDERS = "singleBookFolders";
    private static final Communication communication = Communication.getInstance();
    private static final String PREF_KEY_DISPLAY_MODE = "displayMode";
    private static PrefsManager instance;
    @NonNull
    private final Context c;
    private final SharedPreferences sp;

    private PrefsManager(@NonNull Context c) {
        PreferenceManager.setDefaultValues(c, R.xml.preferences, false);
        this.c = c;
        sp = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static synchronized PrefsManager getInstance(@NonNull Context c) {
        if (instance == null) {
            instance = new PrefsManager(c.getApplicationContext());
        }
        return instance;
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
        Set<String> set = sp.getStringSet(PREF_KEY_COLLECTION_FOLDERS, new HashSet<String>(10));
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
        Set<String> set = sp.getStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, new HashSet<String>(10));
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
        return sp.getInt(c.getString(R.string.pref_key_sleep_time), 20);
    }

    /**
     * Returns the time after which the sleep timer should pause the book.
     *
     * @param time sleep time in Minutes
     */
    public synchronized void setSleepTime(int time) {
        sp.edit().putInt(c.getString(R.string.pref_key_sleep_time), time)
                .apply();
    }

    /**
     * Returns the time to seek when pressing a skip button.
     *
     * @return the time to seek
     */
    public synchronized int getSeekTime() {
        return sp.getInt(c.getString(R.string.pref_key_seek_time), 20);
    }

    /**
     * Sets the time to seek when pressing a skip button
     *
     * @param time the time to seek
     */
    public synchronized void setSeekTime(int time) {
        sp.edit().putInt(c.getString(R.string.pref_key_seek_time), time)
                .apply();
    }

    /**
     * @return true if the player should resume after the headset has been replugged. (If previously
     * paused by unplugging).
     */
    public synchronized boolean resumeOnReplug() {
        return sp.getBoolean(c.getString(R.string.pref_key_resume_on_replug), true);
    }

    /**
     * @return true if should pause the player on a temporary interruption.
     */
    public synchronized boolean pauseOnTempFocusLoss() {
        return sp.getBoolean(c.getString(R.string.pref_key_pause_on_can_duck), false);
    }

    /**
     * Returns the amount to rewind after the player was paused manually.
     *
     * @return the rewind amount
     */
    public synchronized int getAutoRewindAmount() {
        return sp.getInt(c.getString(R.string.pref_key_auto_rewind), 2);
    }

    /**
     * Sets the time the player should rewind on manual pausing.
     *
     * @param autoRewindAmount the amount to auto rewind
     */
    public synchronized void setAutoRewindAmount(int autoRewindAmount) {
        sp.edit().putInt(c.getString(R.string.pref_key_auto_rewind), autoRewindAmount)
                .apply();
    }

    /**
     * @return true if a {@link de.ph1b.audiobook.model.Bookmark} should be set each time the sleep
     * timer is called
     */
    public synchronized boolean setBookmarkOnSleepTimer() {
        return sp.getBoolean(c.getString(R.string.pref_key_bookmark_on_sleep), false);
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
