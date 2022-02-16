package voice.playbackScreen

import voice.data.Book

interface BookPlayNavigator {

  fun toSettings()
  fun toChangePlaybackSpeed()
  fun toBookmarkDialog(id: Book.Id)
  fun toSelectChapters(id: Book.Id)
}
