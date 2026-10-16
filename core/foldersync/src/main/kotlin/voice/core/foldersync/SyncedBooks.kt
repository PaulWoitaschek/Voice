package voice.core.foldersync

import voice.core.data.Book
import voice.core.data.BookContent
import java.time.Instant

/**
 * What this device has to say about the copies of one book. A part keeps its time from [own], the entry this device
 * wrote last, unless it changed since.
 */
internal fun List<Book>.toSyncedBook(
  own: SyncedBook?,
  ownFileExists: Boolean,
  now: Long,
): SyncedBook {
  val content = maxBy { it.content.lastPlayedAt }.content
  return SyncedBook(
    place = content.place(),
    name = content.syncedName(own?.name, ownFileExists, now),
  )
}

private fun BookContent.place(): SyncedPlace? {
  if (lastPlayedAt == Instant.EPOCH) return null
  return SyncedPlace(
    file = currentChapterIndex,
    position = positionInChapter,
    speed = playbackSpeed,
    playedAt = lastPlayedAt.toEpochMilli(),
  )
}

private fun BookContent.syncedName(
  own: SyncedText?,
  ownFileExists: Boolean,
  now: Long,
): SyncedText? = when {
  // the files' name can come from a folder name that differs between devices, and a name equal to it isn't kept
  own != null -> if ((own.value ?: metadata.name) == name) own else SyncedText(customName, now)
  customName == null -> null
  // a name given before this device synced the folder loses against one changed since
  else -> SyncedText(customName, changedAt = if (ownFileExists) now else 0)
}

/**
 * Takes each part of [other] that changed later than this one.
 */
internal fun SyncedBook.with(other: SyncedBook): SyncedBook = SyncedBook(
  place = newer(place, other.place) { it.playedAt },
  name = newer(name, other.name) { it.changedAt },
)

private inline fun <T : Any> newer(
  mine: T?,
  other: T?,
  time: (T) -> Long,
): T? = when {
  other == null -> mine
  mine == null || time(other) > time(mine) -> other
  else -> mine
}
