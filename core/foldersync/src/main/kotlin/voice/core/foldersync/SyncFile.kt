package voice.core.foldersync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The places one device listened to, in `.voice/sync-<device>.json` inside a library folder. Books are known by their
 * [voice.core.data.syncKey], because their ids differ on every device.
 */
@Serializable
internal data class SyncFile(val books: Map<String, SyncedPosition>)

/**
 * @param file the index of the audio file in the book
 * @param position the position in that file, in ms
 * @param updatedAt when the book was last played, in ms since the epoch
 */
@Serializable
internal data class SyncedPosition(
  val file: Int,
  val position: Long,
  val updatedAt: Long,
)

private val json = Json { ignoreUnknownKeys = true }

internal fun SyncFile.encode(): String = json.encodeToString(this)

// a sync tool can leave a file half-written
internal fun parseSyncFile(text: String): SyncFile? = try {
  json.decodeFromString<SyncFile>(text)
} catch (_: IllegalArgumentException) {
  null
}
