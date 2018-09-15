package de.ph1b.audiobook.koin

import com.squareup.moshi.Moshi
import de.paulwoitaschek.chapterreader.ChapterReaderFactory
import de.ph1b.audiobook.covercolorextractor.CoverColorExtractor
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.features.audio.Equalizer
import de.ph1b.audiobook.features.audio.LoudnessGain
import de.ph1b.audiobook.features.bookOverview.BookOverviewViewModel
import de.ph1b.audiobook.features.bookSearch.BookSearchHandler
import de.ph1b.audiobook.features.bookSearch.BookSearchParser
import de.ph1b.audiobook.features.bookmarks.BookmarkPresenter
import de.ph1b.audiobook.features.folderChooser.StorageDirFinder
import de.ph1b.audiobook.features.widget.TriggerWidgetOnChange
import de.ph1b.audiobook.features.widget.WidgetUpdater
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.DurationAnalyzer
import de.ph1b.audiobook.misc.MediaAnalyzer
import de.ph1b.audiobook.misc.MetaDataAnalyzer
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.ShakeDetector
import de.ph1b.audiobook.playback.SleepTimer
import de.ph1b.audiobook.playback.utils.BookUriConverter
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.playback.utils.MediaBrowserHelper
import de.ph1b.audiobook.playback.utils.NotificationChannelCreator
import de.ph1b.audiobook.playback.utils.WakeLockManager
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import de.ph1b.audiobook.uitools.ImageHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent
import java.util.UUID

const val PLAYBACK_SERVICE_SCOPE = "koin#scope#playbackService"

fun ModuleDefinition.currentBookIdPref() = get<Pref<UUID>>(PrefKeys.CURRENT_BOOK)
fun ModuleDefinition.autoRewindAmountPref() = get<Pref<Int>>(PrefKeys.AUTO_REWIND_AMOUNT)
fun ModuleDefinition.seekTimePref() = get<Pref<Int>>(PrefKeys.SEEK_TIME)
fun ModuleDefinition.resumeAfterCallPref() = get<Pref<Boolean>>(PrefKeys.RESUME_AFTER_CALL)

val AppModule = module {
  factory { BookOverviewViewModel(get(), get(), get(), get(), get(), currentBookIdPref()) }
  single { PlayStateManager() }
  single { PlayerController(androidContext()) }
  single { CoverFromDiscCollector(get(), get()) }
  single { ImageHelper(get()) }
  single {
    BookAdder(
      get(),
      get(),
      get(),
      get(),
      get(),
      get(PrefKeys.SINGLE_BOOK_FOLDERS),
      get(PrefKeys.COLLECTION_BOOK_FOLDERS)
    )
  }
  factory { MediaAnalyzer(get(), get()) }
  factory { DurationAnalyzer(get(), get()) }
  factory { MetaDataAnalyzer() }
  factory { DataSourceConverter(get()) }
  single { ChapterReaderFactory.create() }
  factory { BookUriConverter() }
  single { AndroidAutoConnectedReceiver() }
  single { Equalizer(get()) }
  single { SleepTimer(get(), get(), get(), get(PrefKeys.SHAKE_TO_RESET), get(PrefKeys.SLEEP_TIME)) }
  single { ShakeDetector(get()) }
  single { BookSearchParser() }
  single { BookSearchHandler(get(), get(), currentBookIdPref()) }
  single { LoudnessGain() }
  single { WakeLockManager(get()) }
  single { NotificationChannelCreator(get(), get()) }
  single { StorageDirFinder(get()) }
  factory { BookmarkPresenter(currentBookIdPref(), get(), get(), get(), get()) }
  single { Moshi.Builder().build() }
  single { TriggerWidgetOnChange(currentBookIdPref(), get(), get(), get()) }
  single { WidgetUpdater(get(), get(), currentBookIdPref(), get(), get(), get(), get()) }
  single { CoverColorExtractor() }
  single { MediaBrowserHelper(get(), get(), currentBookIdPref(), get()) }
}


val K = object : KoinComponent {}
