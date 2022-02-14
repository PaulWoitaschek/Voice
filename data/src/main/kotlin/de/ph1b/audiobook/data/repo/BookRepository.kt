package de.ph1b.audiobook.data.repo

import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
@Inject constructor(
  private val chapterRepo: ChapterRepo,
  private val contentRepo: BookContentRepo,
) {

  fun flow(): Flow<List<Book>> {
    return contentRepo.flow()
      .map { contents ->
        contents.filter { it.isActive }
          .map { content ->
            Book(content, content.chapters.map { chapterRepo.get(it)!! })
          }
      }
  }

  private suspend fun BookContent.book(): Book {
    return Book(
      content = this,
      chapters = chapters.map { chapterId ->
        chapterRepo.get(chapterId)!!
      }
    )
  }

  fun flow(id: Book.Id): Flow<Book?> {
    return contentRepo.flow(id)
      .map { it?.book() }
  }

  suspend fun updateBook(id: Book.Id, update: (BookContent) -> BookContent) {
    val content = contentRepo.flow(id).first() ?: return
    val updated = update(content)
    contentRepo.put(updated)
  }
}
