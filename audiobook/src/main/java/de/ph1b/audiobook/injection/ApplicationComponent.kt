package de.ph1b.audiobook.injection

import android.content.Context
import dagger.Component
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.features.BookActivity
import de.ph1b.audiobook.features.book_overview.*
import de.ph1b.audiobook.features.book_playing.BookPlayController
import de.ph1b.audiobook.features.book_playing.JumpToPositionDialogFragment
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment
import de.ph1b.audiobook.features.book_playing.SleepTimerDialogFragment
import de.ph1b.audiobook.features.bookmarks.BookmarkDialogFragment
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity
import de.ph1b.audiobook.features.folder_chooser.FolderChooserPresenter
import de.ph1b.audiobook.features.folder_overview.FolderOverviewPresenter
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.features.widget.WidgetUpdateService
import de.ph1b.audiobook.playback.PlaybackService
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import de.ph1b.audiobook.uitools.CoverReplacement
import javax.inject.Singleton

/**
 * Base component that is the entry point for injection.
 *
 * @author Paul Woitaschek
 */
@Singleton
@Component(modules = arrayOf(BaseModule::class, AndroidModule::class, PrefsModule::class))
interface ApplicationComponent {

  val bookShelfPresenter: BookShelfPresenter
  val context: Context

  fun inject(target: App)
  fun inject(target: AutoRewindDialogFragment)
  fun inject(target: BaseActivity)
  fun inject(target: PlaybackService)
  fun inject(target: BookActivity)
  fun inject(target: BookShelfAdapter)
  fun inject(target: BookmarkDialogFragment)
  fun inject(target: BookPlayController)
  fun inject(target: BookShelfController)
  fun inject(target: ChangeNotifier)
  fun inject(target: CoverReplacement)
  fun inject(target: SettingsController)
  fun inject(target: EditBookTitleDialogFragment)
  fun inject(target: EditBookBottomSheet)
  fun inject(target: EditCoverDialogFragment)
  fun inject(target: FolderChooserActivity)
  fun inject(target: FolderChooserPresenter)
  fun inject(target: FolderOverviewPresenter)
  fun inject(target: ImagePickerController)
  fun inject(target: JumpToPositionDialogFragment)
  fun inject(target: PlaybackSpeedDialogFragment)
  fun inject(target: SeekDialogFragment)
  fun inject(target: SleepTimerDialogFragment)
  fun inject(target: ThemePickerDialogFragment)
  fun inject(target: WidgetUpdateService)
}