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

  fun flow(): Flow<List<BookContent2>> = contentRepo.flow()

  suspend fun updateBook(content: BookContent2) {
    contentRepo.put(content)
  }

  fun flow(uri: Uri): Flow<Book2?> = contentRepo.flow()
    .map { contents ->
      val content = contents.find { it.uri == uri } ?: return@map null
      val chapters = content.chapters.map { chapterUri ->
        chapterRepo.get(chapterUri) ?: error("Chapter for $chapterUri not found")
      }
      Book2(content = content, chapters = chapters)
    }
}
