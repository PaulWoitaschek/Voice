package voice.core.audiobookshelf

import voice.core.data.AUDIOBOOKSHELF_SCHEME
import voice.core.data.BookId
import voice.core.data.ChapterId

private const val ITEM_PREFIX = "$AUDIOBOOKSHELF_SCHEME://item/"

/**
 * Server books keep their ids whether they are streamed or downloaded, so progress, bookmarks and the listening
 * history stay with them. The track index comes first so the chapters sort in the order of the book.
 */
internal object AudiobookshelfIds {

  fun bookId(itemId: String): BookId = BookId("$ITEM_PREFIX$itemId")

  fun chapterId(
    itemId: String,
    trackIndex: Int,
    ino: String,
  ): ChapterId = ChapterId("$ITEM_PREFIX$itemId/track/$trackIndex/$ino")
}

internal val BookId.itemId: String?
  get() = value.takeIf { it.startsWith(ITEM_PREFIX) }
    ?.removePrefix(ITEM_PREFIX)
    ?.takeIf { it.isNotEmpty() && '/' !in it }

internal data class TrackRef(
  val itemId: String,
  val ino: String,
)

internal fun trackRef(uri: String): TrackRef? {
  if (!uri.startsWith(ITEM_PREFIX)) return null
  val segments = uri.removePrefix(ITEM_PREFIX).split('/')
  if (segments.size != 4 || segments[1] != "track") return null
  val itemId = segments[0].takeIf { it.isNotEmpty() } ?: return null
  val ino = segments[3].takeIf { it.isNotEmpty() } ?: return null
  return TrackRef(itemId = itemId, ino = ino)
}

internal val ChapterId.trackRef: TrackRef? get() = trackRef(value)
