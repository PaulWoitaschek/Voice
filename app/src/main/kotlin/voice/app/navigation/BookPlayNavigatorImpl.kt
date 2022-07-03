package voice.app.navigation

import com.squareup.anvil.annotations.ContributesBinding
import voice.app.features.audio.PlaybackSpeedDialogController
import voice.app.features.bookPlaying.selectchapter.SelectChapterDialog
import voice.app.features.bookmarks.BookmarkController
import voice.common.AppScope
import voice.data.Book
import voice.playbackScreen.BookPlayNavigator
import voice.settings.SettingsController
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class BookPlayNavigatorImpl
@Inject constructor(
  private val navigator: Navigator
) : BookPlayNavigator {

  override fun toSettings() {
    navigator.push(SettingsController())
  }

  override fun toChangePlaybackSpeed() {
    navigator.push(PlaybackSpeedDialogController())
  }

  override fun toBookmarkDialog(id: Book.Id) {
    navigator.push(BookmarkController(id))
  }

  override fun toSelectChapters(id: Book.Id) {
    navigator.push(SelectChapterDialog(id))
  }
}

