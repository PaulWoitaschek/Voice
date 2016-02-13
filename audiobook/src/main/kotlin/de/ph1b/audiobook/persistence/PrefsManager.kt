/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.persistence

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.uitools.ThemeUtil
import de.ph1b.audiobook.view.fragment.BookShelfFragment
import rx.subjects.BehaviorSubject
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Preference manager, managing the setting and getting of [SharedPreferences]

 * @author Paul Woitaschek
 */
@Singleton
class PrefsManager
@Inject
constructor(c: Context, @Named(FOR) private val sp: SharedPreferences) {

    private val PREF_KEY_RESUME_ON_REPLUG: String
    private val PREF_KEY_THEME: String
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
     * an observable with the id of the current book, or [Book.ID_UNKNOWN] if there is none.
     */
    val currentBookId: BehaviorSubject<Long>

    init {
        PreferenceManager.setDefaultValues(c, R.xml.preferences, false)

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
        @Synchronized set(theme) = sp.edit { setString(PREF_KEY_THEME to theme.name) }

    /**
     * Sets the current bookId.

     * @param bookId the book Id to set
     */
    @Synchronized fun setCurrentBookId(bookId: Long) {
        sp.edit { setLong(PREF_KEY_CURRENT_BOOK to bookId) }
        currentBookId.onNext(bookId)
    }


    /**
     * All book paths that are set as [Book.Type.COLLECTION_FOLDER] or
     * [Book.Type.COLLECTION_FILE]
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
            sp.edit { setStringSet(PREF_KEY_COLLECTION_FOLDERS to set) }
            App.component().bookAdder.scanForFiles(true)
        }

    /**
     * Like [PrefsManager.collectionFolders] but with
     * [Book.Type.SINGLE_FILE] or
     * [Book.Type.SINGLE_FOLDER].
     */
    var singleBookFolders: List<String>
        @Synchronized get() {
            val set = sp.getStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, HashSet<String>(10))
            return ArrayList(set)
        }
        @Synchronized set(folders) {
            val set = HashSet<String>(folders.size)
            set.addAll(folders)
            sp.edit { setStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS to set) }
            App.component().bookAdder.scanForFiles(true)
        }


    /**
     * The time to sleep after which the player should pause the book when sleep timer has
     * been activated
     */
    var sleepTime: Int
        @Synchronized get() = sp.getInt(PREF_KEY_SLEEP_TIME, 20)
        @Synchronized set(time) = sp.edit { setInt(PREF_KEY_SLEEP_TIME to time) }


    /**
     * The time to seek when pressing a skip button. (in seconds.)
     */
    var seekTime: Int
        @Synchronized get() = sp.getInt(PREF_KEY_SEEK_TIME, 20)
        @Synchronized set(time) = sp.edit { setInt(PREF_KEY_SEEK_TIME to time) }

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
        @Synchronized set(autoRewindAmount) = sp.edit { setInt(PREF_KEY_AUTO_REWIND to autoRewindAmount) }

    /**
     * @return true if a [Bookmark] should be set each time the sleep
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
        @Synchronized set(displayMode) = sp.edit { setString(PREF_KEY_DISPLAY_MODE to displayMode.name) }

    companion object {
        const val FOR = "forPrefsManager"
    }

}
