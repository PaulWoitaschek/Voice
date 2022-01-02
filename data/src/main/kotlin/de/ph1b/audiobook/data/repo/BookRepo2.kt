package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.Book2
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepo2
@Inject constructor(
  private val contentRepo: BookContentRepo,
  private val chapterRepo: ChapterRepo,
) {

  fun book(uri: Uri): Book2? {
    val content = contentRepo[uri] ?: return null
    val chapters = content.chapters.mapNotNull { chapterRepo.get(uri) }
    return Book2(content, chapters = chapters)
  }
}
