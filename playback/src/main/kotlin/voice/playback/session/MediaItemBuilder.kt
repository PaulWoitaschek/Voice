package voice.playback.session

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import voice.data.ChapterMark

internal enum class MediaType {
  AudioBook, AudioBookChapter, AudioBookRoot
}

internal fun MediaItem(
  title: String,
  mediaId: MediaId,
  isPlayable: Boolean,
  browsable: Boolean,
  album: String? = null,
  artist: String? = null,
  genre: String? = null,
  sourceUri: Uri? = null,
  imageUri: Uri? = null,
  extras: Bundle = Bundle.EMPTY,
  mediaType: MediaType,
): MediaItem {
  val metadata =
    MediaMetadata.Builder()
      .setAlbumTitle(album)
      .setTitle(title)
      .setArtist(artist)
      .setGenre(genre)
      .setIsBrowsable(browsable)
      .setIsPlayable(isPlayable)
      .setArtworkUri(imageUri)
      .setExtras(extras)
      .setMediaType(
        when (mediaType) {
          MediaType.AudioBook -> MediaMetadata.MEDIA_TYPE_AUDIO_BOOK
          MediaType.AudioBookChapter -> MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
          MediaType.AudioBookRoot -> MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS
        },
      )
      .build()

  return MediaItem.Builder()
    .setMediaId(Json.encodeToString(MediaId.serializer(), mediaId))
    .setMediaMetadata(metadata)
    .setUri(sourceUri)
    .build()
}

internal fun MediaItem.chapterMarks(): List<ChapterMark> {
  return Json.decodeFromString(
    deserializer = ListSerializer(ChapterMark.serializer()),
    string = mediaMetadata.extras!!.getString(EXTRA_CHAPTER_MARKS)!!,
  )
}

internal fun mediaItemChapterMarkExtras(chapterMarks: List<ChapterMark>): Bundle {
  return Bundle().apply {
    putString(
      EXTRA_CHAPTER_MARKS,
      Json.encodeToString(ListSerializer(ChapterMark.serializer()), chapterMarks),
    )
  }
}

private const val EXTRA_CHAPTER_MARKS = "chapterMarks"

fun String.toMediaIdOrNull(): MediaId? =
  try {
    Json.decodeFromString(MediaId.serializer(), this)
  } catch (e: SerializationException) {
    null
  }
