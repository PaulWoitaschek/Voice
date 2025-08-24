package voice.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.logging.core.Logger

@SingleIn(AppScope::class)
@Inject
public class BookRepository
internal constructor(
  private val chapterRepo: ChapterRepo,
  private val contentRepo: BookContentRepo,
) {

  private var warmedUp = false
  private val mutex = Mutex()

  private suspend fun warmUp() {
    if (warmedUp) return
    mutex.withLock {
      if (warmedUp) return@withLock
      val chapters = contentRepo.all()
        .filter { it.isActive }
        .flatMap { it.chapters }
      chapterRepo.warmup(chapters)
      warmedUp = true
    }
  }

  public fun flow(): Flow<List<Book>> {
    return contentRepo.flow()
      .map { contents ->
        contents.filter { it.isActive }
          .mapNotNull { content ->
            content.book()
          }
      }
  }

  public suspend fun all(): List<Book> {
    return contentRepo.all()
      .filter { it.isActive }
      .mapNotNull { it.book() }
  }

  public fun flow(id: BookId): Flow<Book?> {
    return contentRepo.flow(id)
      .map { it?.book() }
  }

  public suspend fun get(id: BookId): Book? {
    return contentRepo.get(id)?.book()
  }

  public suspend fun updateBook(
    id: BookId,
    update: (BookContent) -> BookContent,
  ) {
    mutex.withLock {
      val content = contentRepo.get(id) ?: return
      val updated = update(content)
      contentRepo.put(updated)
    }
  }

  private suspend fun BookContent.book(): Book? {
    warmUp()
    return Book(
      content = this,
      chapters = chapters.map { chapterId ->
        val chapter = chapterRepo.get(chapterId)
        if (chapter == null) {
          Logger.w("Missing chapter with id=$chapterId for $this")
          return null
        }
        chapter
      },
    )
  }
}
