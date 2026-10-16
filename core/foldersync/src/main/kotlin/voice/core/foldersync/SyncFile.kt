package voice.core.foldersync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import voice.core.data.Bookmark
import kotlin.uuid.Uuid

/**
 * What one device knows about its books, in `.voice/sync-<device>.json` inside a library folder. Books are known by
 * their [voice.core.data.syncKey], because their ids differ on every device.
 */
@Serializable
internal data class SyncFile(val books: Map<String, SyncedBook>)

/**
 * Every part has its own time, so an app that doesn't know a part yet never overwrites it.
 *
 * @param deletedBookmarks when each bookmark was deleted, by its id, so another device doesn't bring it back
 */
@Serializable
internal data class SyncedBook(
  val place: SyncedPlace? = null,
  val name: SyncedText? = null,
  val bookmarks: Map<Uuid, SyncedBookmark> = emptyMap(),
  val deletedBookmarks: Map<Uuid, Long> = emptyMap(),
)

/**
 * @param file the index of the audio file in the book
 * @param position the position in that file, in ms
 * @param playedAt when the book was last played, in ms since the epoch
 */
@Serializable
internal data class SyncedPlace(
  val file: Int,
  val position: Long,
  val speed: Float,
  val playedAt: Long,
)

/**
 * @param value what the listener entered, or null for the value from the files
 * @param changedAt when the listener changed it, in ms since the epoch
 */
@Serializable
internal data class SyncedText(
  val value: String?,
  val changedAt: Long,
)

/**
 * @param file the index of the audio file in the book
 * @param time the position in that file, in ms
 * @param changedAt when the listener added or last changed it, in ms since the epoch
 */
@Serializable
internal data class SyncedBookmark(
  val file: Int,
  val time: Long,
  val title: String? = null,
  val kind: Bookmark.Kind = Bookmark.Kind.Note,
  val addedAt: Long,
  val changedAt: Long,
)

private val json = Json {
  ignoreUnknownKeys = true
  // a bookmark kind this version doesn't know yet reads as a note
  coerceInputValues = true
}

internal fun SyncFile.encode(): String = json.encodeToString(this)

// a sync tool can leave a file half-written
internal fun parseSyncFile(text: String): SyncFile? = try {
  json.decodeFromString<SyncFile>(text)
} catch (_: IllegalArgumentException) {
  null
}
