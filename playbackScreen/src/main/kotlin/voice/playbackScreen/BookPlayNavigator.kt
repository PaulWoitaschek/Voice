package voice.playbackScreen

import de.ph1b.audiobook.data.Book2

interface BookPlayNavigator {

  fun toSettings()
  fun toChangePlaybackSpeed()
  fun toBookmarkDialog(id: Book2.Id)
  fun toSelectChapters(id: Book2.Id)
}
