package de.ph1b.audiobook.injection

import android.app.Application
import android.content.Context
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.audio.PlaybackSpeedDialogController
import de.ph1b.audiobook.features.bookCategory.BookCategoryController
import de.ph1b.audiobook.features.bookOverview.BookOverviewController
import de.ph1b.audiobook.features.bookOverview.EditBookTitleDialogController
import de.ph1b.audiobook.features.bookOverview.EditCoverDialogController
import de.ph1b.audiobook.features.bookPlaying.selectchapter.SelectChapterDialog
import de.ph1b.audiobook.features.bookmarks.BookmarkPresenter
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.widget.BaseWidgetProvider
import de.ph1b.audiobook.playback.di.PlaybackComponent
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class
)
interface AppComponent {

  val bookmarkPresenter: BookmarkPresenter
  val context: Context

  fun inject(target: App)
  fun inject(target: BaseWidgetProvider)
  fun inject(target: BookCategoryController)
  fun inject(target: BookOverviewController)
  fun inject(target: CoverFromInternetController)
  fun inject(target: SelectChapterDialog)
  fun inject(target: EditBookTitleDialogController)
  fun inject(target: EditCoverDialogController)
  fun inject(target: MainActivity)
  fun inject(target: PlaybackSpeedDialogController)

  fun playbackComponentFactory(): PlaybackComponent.Factory

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance application: Application): AppComponent
  }

  companion object {
    fun factory(): Factory = DaggerAppComponent.factory()
  }
}
