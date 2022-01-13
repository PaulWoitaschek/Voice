package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

  fun flow(): Flow<List<BookContent2>> = contentRepo.flow()

  fun flow(uri: Uri): Flow<Book2?> = contentRepo.flow()
    .map { contents ->
      val content = contents.find { it.uri == uri } ?: return@map null
      val chapters = content.chapters.mapNotNull { chapterRepo.get(uri) }
      Book2(content = content, chapters = chapters)
    }
}
