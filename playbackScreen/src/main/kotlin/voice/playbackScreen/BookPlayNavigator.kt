package voice.playbackScreen

import de.ph1b.audiobook.data.Book

interface BookPlayNavigator {

  fun toSettings()
  fun toChangePlaybackSpeed()
  fun toBookmarkDialog(id: Book.Id)
  fun toSelectChapters(id: Book.Id)
}
