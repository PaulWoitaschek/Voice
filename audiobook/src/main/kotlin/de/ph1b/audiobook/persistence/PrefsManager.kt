package de.ph1b.audiobook.persistence

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.interfaces.ForApplication
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.uitools.ThemeUtil
import rx.subjects.BehaviorSubject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preference manager, managing the setting and getting of [SharedPreferences]

 * @author Paul Woitaschek
 */
@Singleton
class PrefsManager
@Inject
constructor(@ForApplication c: Context) {
    private val PREF_KEY_RESUME_ON_REPLUG: String
    private val PREF_KEY_THEME: String
    private val sp: SharedPreferences
    private val PREF_KEY_BOOKMARK_ON_SLEEP: String
    private val PREF_KEY_SEEK_TIME: String
    private val PREF_KEY_SLEEP_TIME: String
    private val PREF_KEY_PAUSE_ON_CAN_DUCK: String
    private val PREF_KEY_AUTO_REWIND: String
    private val PREF_KEY_CURRENT_BOOK = "currentBook"
    private val PREF_KEY_COLLECTION_FOLDERS = "folders"
    private val PREF_KEY_SINGLE_BOOK_FOLDERS = "singleBookFolders"
    private val PREF_KEY_DISPLAY_MODE = "displayMode"


    /**
     * an observable with the id of the current book, or [Book.ID_UNKNOW] if there is none.
     */
    val currentBookId: BehaviorSubject<Long>

    init {
        PreferenceManager.setDefaultValues(c, R.xml.preferences, false)
        sp = PreferenceManager.getDefaultSharedPreferences(c)

        PREF_KEY_THEME = c.getString(R.string.pref_key_theme)
        PREF_KEY_SLEEP_TIME = c.getString(R.string.pref_key_sleep_time)
        PREF_KEY_RESUME_ON_REPLUG = c.getString(R.string.pref_key_resume_on_replug)
        PREF_KEY_SEEK_TIME = c.getString(R.string.pref_key_seek_time)
        PREF_KEY_BOOKMARK_ON_SLEEP = c.getString(R.string.pref_key_bookmark_on_sleep)
        PREF_KEY_AUTO_REWIND = c.getString(R.string.pref_key_auto_rewind)
        PREF_KEY_PAUSE_ON_CAN_DUCK = c.getString(R.string.pref_key_pause_on_can_duck)
        currentBookId = BehaviorSubject.create(sp.getLong(PREF_KEY_CURRENT_BOOK, Book.ID_UNKNOWN.toLong()))
    }

    var theme: ThemeUtil.Theme
        @Synchronized get() {
            val value = sp.getString(PREF_KEY_THEME, null)
            if (value == null) {
                return ThemeUtil.Theme.LIGHT
            } else {
                return ThemeUtil.Theme.valueOf(value)
            }
        }
        @Synchronized set(theme) = sp.edit().putString(PREF_KEY_THEME, theme.name).apply()

    /**
     * Sets the current bookId.

     * @param bookId the book Id to set
     */
    @Synchronized fun setCurrentBookId(bookId: Long) {
        sp.edit().putLong(PREF_KEY_CURRENT_BOOK, bookId).apply()
        currentBookId.onNext(bookId)
    }


    /**
     * All book paths that are set as [de.ph1b.audiobook.model.Book.Type.COLLECTION_FOLDER] or
     * [de.ph1b.audiobook.model.Book.Type.COLLECTION_FILE]
     *
     * @see PrefsManager.collectionFolders
     */
    var collectionFolders: List<String>
        @Synchronized get() {
            val set = sp.getStringSet(PREF_KEY_COLLECTION_FOLDERS, HashSet<String>(10))
            return ArrayList(set)
        }
        @Synchronized set(folders) {
            val set = HashSet<String>(folders.size)
            set.addAll(folders)
            sp.edit().putStringSet(PREF_KEY_COLLECTION_FOLDERS, set).apply()
        }

    /**
     * Like [PrefsManager.collectionFolders] but with
     * [de.ph1b.audiobook.model.Book.Type.SINGLE_FILE] or
     * [de.ph1b.audiobook.model.Book.Type.SINGLE_FOLDER].
     */
    var singleBookFolders: List<String>
        @Synchronized get() {
            val set = sp.getStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, HashSet<String>(10))
            return ArrayList(set)
        }
        @Synchronized set(folders) {
            val set = HashSet<String>(folders.size)
            set.addAll(folders)
            sp.edit().putStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, set).apply()
        }


    /**
     * The time to sleep after which the player should pause the book when sleep timer has
     * been activated
     */
    var sleepTime: Int
        @Synchronized get() = sp.getInt(PREF_KEY_SLEEP_TIME, 20)
        @Synchronized set(time) = sp.edit().putInt(PREF_KEY_SLEEP_TIME, time).apply()


    /**
     * The time to seek when pressing a skip button. (in seconds.)
     */
    var seekTime: Int
        @Synchronized get() = sp.getInt(PREF_KEY_SEEK_TIME, 20)
        @Synchronized set(time) = sp.edit().putInt(PREF_KEY_SEEK_TIME, time).apply()

    /**
     * @return true if the player should resume after the headset has been replugged. (If previously
     * paused by unplugging).
     */
    @Synchronized fun resumeOnReplug(): Boolean {
        return sp.getBoolean(PREF_KEY_RESUME_ON_REPLUG, true)
    }

    /**
     * @return true if should pause the player on a temporary interruption.
     */
    @Synchronized fun pauseOnTempFocusLoss(): Boolean {
        return sp.getBoolean(PREF_KEY_PAUSE_ON_CAN_DUCK, false)
    }

    /**
     * The amount to rewind after the player was paused manually.
     */
    var autoRewindAmount: Int
        @Synchronized get() = sp.getInt(PREF_KEY_AUTO_REWIND, 2)
        @Synchronized set(autoRewindAmount) = sp.edit().putInt(PREF_KEY_AUTO_REWIND, autoRewindAmount).apply()

    /**
     * @return true if a [de.ph1b.audiobook.model.Bookmark] should be set each time the sleep
     * * timer is called
     */
    @Synchronized fun setBookmarkOnSleepTimer(): Boolean {
        return sp.getBoolean(PREF_KEY_BOOKMARK_ON_SLEEP, false)
    }


    /**
     * The display mode that has been set or the default.
     */
    var displayMode: BookShelfFragment.DisplayMode
        @Synchronized get() = BookShelfFragment.DisplayMode.valueOf(sp.getString(PREF_KEY_DISPLAY_MODE, BookShelfFragment.DisplayMode.GRID.name))
        @Synchronized set(displayMode) = sp.edit().putString(PREF_KEY_DISPLAY_MODE, displayMode.name).apply()

}
