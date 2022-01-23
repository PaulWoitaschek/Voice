package de.ph1b.audiobook.data.repo

import android.net.Uri
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.repo.internals.dao.BookContent2Dao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepo2
@Inject constructor(
  private val chapterRepo: ChapterRepo,
  private val contentDao: BookContent2Dao,
) {

  private val cacheMutex = Mutex()
  private var cacheFilled = false

  private val cache = MutableStateFlow<List<Book2>>(emptyList())

  private suspend fun fillCache() {
    if (cacheFilled) {
      return
    }
    cacheMutex.withLock {
      val contents = contentDao.all(isActive = true)
        .map { content ->
          val chapters = content.chapters.map { chapterUri ->
            chapterRepo.get(chapterUri) ?: error("Chapter for $chapterUri not found")
          }
          Book2(content, chapters)
        }
      cache.emit(contents)
      cacheFilled = true
    }
  }

  fun flow(): Flow<List<Book2>> {
    return cache.onStart { fillCache() }
  }

  suspend fun setAllInactiveExcept(uris: List<Uri>) {
    fillCache()

    val currentBooks = cache.value

    val (active, inactive) = currentBooks.partition { it.id in uris }
    contentDao.insert(
      inactive.map { book ->
        book.content.copy(isActive = false)
      }
    )
    cache.value = active
  }

  suspend fun updateBook(content: BookContent2) {
    fillCache()
    cache.update {
      it.toMutableList().apply {
        val book = find { book ->
          book.id == content.uri
        }
        if (book != null) {
          remove(book)
          add(book.copy(content = content))
        }
      }
    }
    contentDao.insert(content)
  }

  fun flow(uri: Uri): Flow<Book2?> {
    return flow().map { books ->
      books.find { it.id == uri }
    }
  }
}
