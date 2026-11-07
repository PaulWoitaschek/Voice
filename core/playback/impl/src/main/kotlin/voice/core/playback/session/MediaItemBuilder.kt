package voice.core.playback.session

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal enum class MediaType {
  AudioBook,
  AudioBookChapter,
  AudioBookFolder,
}

internal fun MediaItem(
  title: String,
  mediaId: MediaId,
  isPlayable: Boolean,
  browsable: Boolean,
  album: String? = null,
  artist: String? = null,
  subtitle: String? = null,
  genre: String? = null,
  sourceUri: Uri? = null,
  imageUri: Uri? = null,
  durationMs: Long? = null,
  clippingConfiguration: ClippingConfiguration = ClippingConfiguration.UNSET,
  mediaType: MediaType,
  extras: Bundle? = null,
): MediaItem {
  val metadata =
    MediaMetadata.Builder()
      .setAlbumTitle(album)
      .setTitle(title)
      // with a display title, Android Auto shows the subtitle instead of the artist
      .setDisplayTitle(title.takeIf { subtitle != null })
      .setSubtitle(subtitle)
      .setArtist(artist)
      .setGenre(genre)
      .setIsBrowsable(browsable)
      .setIsPlayable(isPlayable)
      .setArtworkUri(imageUri)
      .setDurationMs(durationMs)
      .setMediaType(
        when (mediaType) {
          MediaType.AudioBook -> MediaMetadata.MEDIA_TYPE_AUDIO_BOOK
          MediaType.AudioBookChapter -> MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
          MediaType.AudioBookFolder -> MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS
        },
      )
      .setExtras(extras)
      .build()

  return MediaItem.Builder()
    .setMediaId(mediaId.encode())
    .setMediaMetadata(metadata)
    .setUri(sourceUri)
    .setClippingConfiguration(clippingConfiguration)
    .build()
}

internal fun MediaId.encode(): String = Json.encodeToString(MediaId.serializer(), this)

fun String.toMediaIdOrNull(): MediaId? =
  try {
    Json.decodeFromString(MediaId.serializer(), this)
  } catch (_: SerializationException) {
    null
  }
