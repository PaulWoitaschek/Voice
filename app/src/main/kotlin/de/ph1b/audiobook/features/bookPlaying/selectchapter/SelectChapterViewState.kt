package de.ph1b.audiobook.features.bookPlaying.selectchapter

import de.ph1b.audiobook.data.ChapterMark

data class SelectChapterViewState(
  val chapters: List<ChapterMark>,
  val selectedIndex: Int?
)
