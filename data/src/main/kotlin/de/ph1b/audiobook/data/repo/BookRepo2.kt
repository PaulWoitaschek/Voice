package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepo2
@Inject constructor(
  private val chapterRepo: ChapterRepo,
  private val contentRepo: BookContentRepo,
) {

  fun flow(): Flow<List<Book2>> {
    return contentRepo.flow()
      .map { contents ->
        contents.filter { it.isActive }
          .map { content ->
            Book2(content, content.chapters.map { chapterRepo.get(it)!! })
          }
      }
  }

  private suspend fun BookContent2.book(): Book2 {
    return Book2(
      content = this,
      chapters = chapters.map { chapterId ->
        chapterRepo.get(chapterId)!!
      }
    )
  }

  fun flow(id: Book2.Id): Flow<Book2?> {
    return contentRepo.flow(id)
      .map { it?.book() }
  }

  suspend fun updateBook(id: Book2.Id, update: (BookContent2) -> BookContent2) {
    val content = contentRepo.flow(id).first() ?: return
    val updated = update(content)
    contentRepo.put(updated)
  }
}
