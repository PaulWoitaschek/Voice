package de.ph1b.audiobook.navigation

import com.squareup.anvil.annotations.ContributesBinding
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.features.audio.PlaybackSpeedDialogController
import de.ph1b.audiobook.features.bookPlaying.selectchapter.SelectChapterDialog
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

  override fun toBookmarkDialog(id: Book2.Id) {
    // todo
  }

  override fun toSelectChapters(id: Book2.Id) {
    navigator.push(SelectChapterDialog(id))
  }
}

