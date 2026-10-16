package voice.core.foldersync

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * What this device has to say about the copies of one book. A part keeps its time from [own], the entry this device
 * wrote last, unless it changed since.
 */
internal fun List<Book>.toSyncedBook(
  bookmarks: Map<ChapterId, List<Bookmark>>,
  own: SyncedBook?,
  ownFileExists: Boolean,
  now: Long,
): SyncedBook {
  val content = played()
  // the bookmarks of every copy, so playing another copy doesn't look like deleting them
  val localBookmarks = flatMap { copy ->
    copy.chapters.flatMapIndexed { file, chapter ->
      bookmarks[chapter.id].orEmpty().map { it.id.value to it.synced(file, own, now) }
    }
  }.toMap()
  val deletedNow = (own?.bookmarks.orEmpty().keys - localBookmarks.keys).associateWith { now }
  return SyncedBook(
    place = content.place(),
    name = content.syncedName(own?.name, ownFileExists, now),
    bookmarks = localBookmarks,
    deletedBookmarks = own?.deletedBookmarks.orEmpty() - localBookmarks.keys + deletedNow,
  )
}

// not a rename, but a device whose folder has another name would take it as one
internal fun SyncedBook.withoutFilesName(copies: List<Book>): SyncedBook {
  return if (name?.value == copies.played().metadata.name) copy(name = null) else this
}

private fun List<Book>.played(): BookContent = maxBy { it.content.lastPlayedAt }.content

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

private fun Bookmark.synced(
  file: Int,
  own: SyncedBook?,
  now: Long,
): SyncedBookmark {
  val known = own?.bookmarks?.get(id.value)
  val synced = SyncedBookmark(
    file = file,
    time = time,
    title = title,
    kind = kind,
    addedAt = addedAt.toEpochMilli(),
    changedAt = known?.changedAt ?: addedAt.toEpochMilli(),
  )
  // undoing a delete brings back a bookmark this device wrote as deleted
  val changed = if (known != null) synced != known else own?.deletedBookmarks?.containsKey(id.value) == true
  return if (changed) synced.copy(changedAt = now) else synced
}

/**
 * Takes each part of [other] that changed later than this one.
 */
internal fun SyncedBook.with(other: SyncedBook): SyncedBook {
  val newerBookmarks = (other.bookmarks.keys + other.deletedBookmarks.keys)
    .filter { other.bookmarkChangedAt(it) > bookmarkChangedAt(it) }
    .toSet()
  return SyncedBook(
    place = newer(place, other.place) { it.playedAt },
    name = newer(name, other.name) { it.changedAt },
    bookmarks = bookmarks - newerBookmarks + other.bookmarks.filterKeys { it in newerBookmarks },
    deletedBookmarks = deletedBookmarks - newerBookmarks + other.deletedBookmarks.filterKeys { it in newerBookmarks },
  )
}

private fun SyncedBook.bookmarkChangedAt(id: Uuid): Long {
  return bookmarks[id]?.changedAt ?: deletedBookmarks[id] ?: Long.MIN_VALUE
}

private inline fun <T : Any> newer(
  mine: T?,
  other: T?,
  time: (T) -> Long,
): T? = when {
  other == null -> mine
  mine == null || time(other) > time(mine) -> other
  else -> mine
}
