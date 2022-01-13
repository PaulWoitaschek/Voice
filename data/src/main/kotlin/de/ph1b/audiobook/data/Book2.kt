package de.ph1b.audiobook.data

data class Book2(
  val content: BookContent2,
  val chapters: List<Chapter2>,
) {

  val currentChapter = chapters.first { it.uri == content.currentChapter }
}
