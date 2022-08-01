package voice.app.features.bookPlaying.selectchapter

import voice.data.ChapterMark

data class SelectChapterViewState(
  val chapters: List<ChapterMark>,
  val selectedIndex: Int?,
)
