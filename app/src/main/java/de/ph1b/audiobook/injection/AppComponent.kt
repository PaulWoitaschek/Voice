package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import de.ph1b.audiobook.data.di.DataComponent
import de.ph1b.audiobook.data.repo.internals.PersistenceModule
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.audio.LoudnessDialog
import de.ph1b.audiobook.features.bookCategory.BookCategoryController
import de.ph1b.audiobook.features.bookOverview.BookOverviewController
import de.ph1b.audiobook.features.bookOverview.EditBookBottomSheetController
import de.ph1b.audiobook.features.bookOverview.EditBookTitleDialogController
import de.ph1b.audiobook.features.bookOverview.EditCoverDialogController
import de.ph1b.audiobook.features.bookOverview.list.LoadBookCover
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.bookPlaying.BookPlayPresenter
import de.ph1b.audiobook.features.bookPlaying.JumpToPositionDialogController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogController
import de.ph1b.audiobook.features.bookPlaying.SleepTimerDialogFragment
import de.ph1b.audiobook.features.bookmarks.BookmarkPresenter
import de.ph1b.audiobook.features.folderChooser.FolderChooserPresenter
import de.ph1b.audiobook.features.folderOverview.FolderOverviewPresenter
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogController
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogController
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogController
import de.ph1b.audiobook.features.widget.BaseWidgetProvider
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayStateManager
import javax.inject.Singleton

/**
 * Base component that is the entry point for injection.
 */
@Singleton
@Component(
  modules = [
    AndroidModule::class,
    PrefsModule::class,
    PersistenceModule::class,
    PlaybackModule::class,
    SortingModule::class
  ]
)
interface AppComponent : DataComponent {

  val bookmarkPresenter: BookmarkPresenter
  val context: Context
  val player: MediaPlayer
  val playStateManager: PlayStateManager

  @Component.Builder
  interface Builder {

    @BindsInstance
    fun application(application: Application): Builder

    fun build(): AppComponent
  }

  fun inject(target: BookCategoryController)
  fun inject(target: App)
  fun inject(target: AutoRewindDialogController)
  fun inject(target: BaseWidgetProvider)
  fun inject(target: BookOverviewController)
  fun inject(target: BookPlayController)
  fun inject(target: BookPlayPresenter)
  fun inject(target: EditBookBottomSheetController)
  fun inject(target: EditBookTitleDialogController)
  fun inject(target: EditCoverDialogController)
  fun inject(target: FolderChooserPresenter)
  fun inject(target: FolderOverviewPresenter)
  fun inject(target: CoverFromInternetController)
  fun inject(target: JumpToPositionDialogController)
  fun inject(target: LoadBookCover)
  fun inject(target: LoudnessDialog)
  fun inject(target: MainActivity)
  fun inject(target: PlaybackSpeedDialogController)
  fun inject(target: SeekDialogController)
  fun inject(target: SettingsController)
  fun inject(target: SleepTimerDialogFragment)
  fun inject(target: ThemePickerDialogController)

  fun playbackComponent(): PlaybackComponent.Builder
}
