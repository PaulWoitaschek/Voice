package voice.core.scanner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.ListeningHistoryRepo

class MemoryListeningHistoryRepo : ListeningHistoryRepo {

  private val events = MutableStateFlow(listOf<ListeningEvent>())

  val all: List<ListeningEvent> get() = events.value

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> {
    return events.map { all -> all.filter { it.bookId == bookId }.sortedByDescending { it.atMillis } }
  }

  override suspend fun add(event: ListeningEvent) {
    events.update { it + event }
  }

  override suspend fun clear() {
    events.value = emptyList()
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
    chapters: Map<ChapterId, ChapterId>,
  ) {
    events.update { all ->
      all.map { event ->
        val chapter = chapters[event.chapterId]
        if (event.bookId in from && chapter != null) {
          event.copy(
            bookId = to,
            chapterId = chapter,
            toChapterId = event.toChapterId?.let { chapters[it] ?: it },
          )
        } else {
          event
        }
      }
    }
  }
}
