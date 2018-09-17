package de.ph1b.audiobook.injection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.bookOverview.EditBookBottomSheet
import de.ph1b.audiobook.features.bookOverview.EditBookTitleDialogFragment
import de.ph1b.audiobook.features.bookOverview.EditCoverDialogFragment
import de.ph1b.audiobook.features.bookPlaying.JumpToPositionDialogFragment
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.bookPlaying.SleepTimerDialogFragment
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.PlaybackSpeedDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.features.widget.BaseWidgetProvider
import de.ph1b.audiobook.playback.PlaybackService

/**
 * Module for dagger bindings
 */
@Module
abstract class BindingModule {

  @ContributesAndroidInjector
  abstract fun mainActivity(): MainActivity

  @ContributesAndroidInjector(modules = arrayOf(PlaybackModule::class))
  @PerService
  abstract fun playbackService(): PlaybackService

  @ContributesAndroidInjector
  abstract fun autoRewindDialogFragment(): AutoRewindDialogFragment

  @ContributesAndroidInjector
  abstract fun editCoverDialogFragment(): EditCoverDialogFragment

  @ContributesAndroidInjector
  abstract fun editBookTitleDialogFragment(): EditBookTitleDialogFragment

  @ContributesAndroidInjector
  abstract fun folderChooserActivity(): FolderChooserActivity

  @ContributesAndroidInjector
  abstract fun jumpToPositionDialogFragment(): JumpToPositionDialogFragment

  @ContributesAndroidInjector
  abstract fun playbackSpeedDialogFragment(): PlaybackSpeedDialogFragment

  @ContributesAndroidInjector
  abstract fun seekDialogFragment(): SeekDialogFragment

  @ContributesAndroidInjector
  abstract fun sleepTimerDialogFragment(): SleepTimerDialogFragment

  @ContributesAndroidInjector
  abstract fun themePickerDialogFragment(): ThemePickerDialogFragment

  @ContributesAndroidInjector
  abstract fun baseWidgetProvider(): BaseWidgetProvider

  @ContributesAndroidInjector
  abstract fun editBookBottomSheet(): EditBookBottomSheet

  @ContributesAndroidInjector
  abstract fun baseActivity(): BaseActivity
}
