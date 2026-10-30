package voice.core.audiobookshelf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import voice.core.data.BookId

/**
 * What the screens around the library need to know about the books on the server.
 */
interface ServerLibrary {

  val syncing: StateFlow<Boolean>

  val reachability: StateFlow<ServerReachability>

  /** Server books that can't play right now: not downloaded while the server can't be reached. */
  val unavailableBooks: Flow<Set<BookId>>

  /**
   * Syncs with the server, unless there is no server. Without [force], a sync that is running or finished less than a
   * minute ago is enough.
   */
  fun sync(force: Boolean = false)

  /** Saves what a listener changed about a server book on the server. */
  suspend fun updateBook(
    bookId: BookId,
    edit: ServerBookEdit,
  ): ServerEdit
}

/**
 * Whether the server can be reached right now, as far as Voice knows.
 */
enum class ServerReachability {
  Unknown,
  Reachable,
  Unreachable,
}

/** What's null stays as the server has it. */
data class ServerBookEdit(
  val title: String? = null,
  val author: String? = null,
  /** Replaces the series Voice shows, together with [part]. The other series the book has on the server stay. */
  val series: String? = null,
  val part: String? = null,
)

enum class ServerEdit {
  Saved,
  NotAllowed,
  Failed,
}
