package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.logging.api.Logger

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class BookRepositoryImpl(
  private val chapterRepo: ChapterRepoImpl,
  private val contentRepo: BookContentRepo,
  private val customMetadataRepo: CustomMetadataRepo,
) : BookRepository {

  private var warmedUp = false
  private val mutex = Mutex()

  private suspend fun warmUp() {
    if (warmedUp) return
    mutex.withLock {
      if (warmedUp) return@withLock
      val chapters = contentRepo.all().filter { it.isActive }.flatMap { it.chapters }
      chapterRepo.warmup(chapters)
      warmedUp = true
    }
  }

  override fun flow(): Flow<List<Book>> {
    return combine(contentRepo.flow(), customMetadataRepo.flow()) { contents, customs ->
      contents
        .filter { it.isActive }
        .mapNotNull { content ->
          content.book(customs[content.id] ?: CustomMetadata(content.id))
        }
    }
  }

  override suspend fun all(): List<Book> {
    return contentRepo
      .all()
      .filter { it.isActive }
      .mapNotNull { it.book(customMetadataRepo.get(it.id)) }
  }

  override fun flow(id: BookId): Flow<Book?> {
    return combine(contentRepo.flow(id), customMetadataRepo.flow(id)) { content, custom ->
      content?.book(custom)
    }
  }

  override suspend fun get(id: BookId): Book? {
    return contentRepo.get(id)?.book(customMetadataRepo.get(id))
  }

  override suspend fun updateBook(
    id: BookId,
    update: (BookContent) -> BookContent,
  ) {
    mutex.withLock {
      val content = contentRepo.get(id) ?: return
      val updated = update(content)
      if (updated != content) {
        contentRepo.put(updated)
      }
    }
  }

  override suspend fun updateCustomMetadata(
    id: BookId,
    update: (CustomMetadata) -> CustomMetadata,
  ) {
    mutex.withLock {
      val content = contentRepo.get(id) ?: return
      val custom = customMetadataRepo.get(id)
      val updated = update(custom).followingFiles(content.metadata)
      if (updated != custom) {
        customMetadataRepo.put(updated)
      }
    }
  }

  private suspend fun BookContent.book(custom: CustomMetadata): Book? {
    warmUp()
    return Book(
      content = this,
      chapters =
        chapters.map { chapterId ->
          val chapter = chapterRepo.get(chapterId)
          if (chapter == null) {
            Logger.w("A book misses one of its ${chapters.size} chapters")
            return null
          }
          chapter
        },
      custom = custom,
    )
  }
}
