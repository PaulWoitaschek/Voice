package de.ph1b.audiobook.persistence

import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.features.bookOverview.BookShelfController
import de.ph1b.audiobook.injection.AutoRewindAmount
import de.ph1b.audiobook.injection.BookmarkOnSleepTimer
import de.ph1b.audiobook.injection.CollectionFolders
import de.ph1b.audiobook.injection.CurrentBookId
import de.ph1b.audiobook.injection.PrefResumeAfterCall
import de.ph1b.audiobook.injection.ResumeOnReplug
import de.ph1b.audiobook.injection.SeekTime
import de.ph1b.audiobook.injection.ShakeToReset
import de.ph1b.audiobook.injection.SingleBookFolders
import de.ph1b.audiobook.injection.SleepTime
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Holds all preferences
 */
@Singleton class PrefsManager
@Inject constructor(
    val theme: Preference<ThemeUtil.Theme>,
    @ResumeOnReplug val resumeOnReplug: Preference<Boolean>,
    @BookmarkOnSleepTimer val bookmarkOnSleepTimer: Preference<Boolean>,
    @ShakeToReset val shakeToReset: Preference<Boolean>,
    @PrefResumeAfterCall val resumeAfterCall: Preference<Boolean>,
    @AutoRewindAmount val autoRewindAmount: Preference<Int>,
    @SeekTime val seekTime: Preference<Int>,
    val displayMode: Preference<BookShelfController.DisplayMode>,
    @SleepTime val sleepTime: Preference<Int>,
    @SingleBookFolders val singleBookFolders: Preference<Set<String>>,
    @CollectionFolders val collectionFolders: Preference<Set<String>>,
    @CurrentBookId val currentBookId: Preference<Long>)
