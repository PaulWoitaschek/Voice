package voice.core.foldersync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What one device knows about its books, in `.voice/sync-<device>.json` inside a library folder. Books are known by
 * their [voice.core.data.syncKey], because their ids differ on every device.
 */
@Serializable
internal data class SyncFile(val books: Map<String, SyncedBook>)

// every part has its own time, so an app that doesn't know a part yet never overwrites it
@Serializable
internal data class SyncedBook(
  val place: SyncedPlace? = null,
  val name: SyncedText? = null,
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

private val json = Json { ignoreUnknownKeys = true }

internal fun SyncFile.encode(): String = json.encodeToString(this)

// a sync tool can leave a file half-written
internal fun parseSyncFile(text: String): SyncFile? = try {
  json.decodeFromString<SyncFile>(text)
} catch (_: IllegalArgumentException) {
  null
}
