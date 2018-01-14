package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import de.ph1b.audiobook.features.audio.LoudnessDialog
import de.ph1b.audiobook.features.bookOverview.BookShelfController
import de.ph1b.audiobook.features.bookOverview.BookShelfPresenter
import de.ph1b.audiobook.features.bookOverview.list.BookShelfHolder
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.bookPlaying.BookPlayPresenter
import de.ph1b.audiobook.features.bookmarks.BookmarkPresenter
import de.ph1b.audiobook.features.folderChooser.FolderChooserPresenter
import de.ph1b.audiobook.features.folderOverview.FolderOverviewPresenter
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.playback.MediaPlayer
import de.ph1b.audiobook.playback.PlayStateManager
import javax.inject.Singleton

/**
 * Base component that is the entry point for injection.
 */
@Singleton
@Component(modules = arrayOf(AndroidModule::class, PrefsModule::class, BindingModule::class, AndroidSupportInjectionModule::class))
interface AppComponent {

  val bookmarkPresenter: BookmarkPresenter
  val bookShelfPresenter: BookShelfPresenter
  val context: Context
  val player: MediaPlayer
  val playStateManager: PlayStateManager

  @Component.Builder
  interface Builder {

    @BindsInstance
    fun application(application: Application): Builder

    fun build(): AppComponent
  }

  fun inject(target: App)
  fun inject(target: BookPlayController)
  fun inject(target: BookPlayPresenter)
  fun inject(target: BookShelfHolder)
  fun inject(target: BookShelfController)
  fun inject(target: FolderChooserPresenter)
  fun inject(target: FolderOverviewPresenter)
  fun inject(target: ImagePickerController)
  fun inject(target: LoudnessDialog)
  fun inject(target: SettingsController)
}
