package voice.core.data.repo

import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.internals.dao.ListeningEventDao
import voice.core.data.repo.internals.transaction

@ContributesBinding(AppScope::class)
public class ListeningHistoryRepoImpl
internal constructor(
  private val dao: ListeningEventDao,
  private val appDb: RoomDatabase,
) : ListeningHistoryRepo {

  override fun events(bookId: BookId): Flow<List<ListeningEvent>> = dao.events(bookId)

  override suspend fun add(event: ListeningEvent) {
    dao.insert(event)
    dao.keepNewest(event.bookId, MAX_EVENTS_PER_BOOK)
  }

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
    chapters: Map<ChapterId, ChapterId>,
  ) {
    if (from.isEmpty() || chapters.isEmpty()) return
    appDb.transaction {
      chapters.forEach { (oldChapterId, newChapterId) ->
        dao.moveChapter(from = from, to = to, oldChapterId = oldChapterId, newChapterId = newChapterId)
      }
      chapters.forEach { (oldChapterId, newChapterId) ->
        if (oldChapterId != newChapterId) {
          dao.moveJumpTarget(bookId = to, oldChapterId = oldChapterId, newChapterId = newChapterId)
        }
      }
      dao.keepNewest(to, MAX_EVENTS_PER_BOOK)
    }
  }

  internal companion object {
    /**
     * Enough for a few dozen listening sessions, while the table stays small.
     */
    const val MAX_EVENTS_PER_BOOK = 250
  }
}
