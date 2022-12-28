package voice.playback.session

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.FOLDER_TYPE_MIXED
import androidx.media3.common.MediaMetadata.FOLDER_TYPE_NONE
import androidx.media3.common.MediaMetadata.FOLDER_TYPE_TITLES
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.ChapterMark
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.toUri
import voice.playback.R
import javax.inject.Inject

class MediaItemProvider
@Inject constructor(
  private val bookRepository: BookRepository,
  private val application: Application,
  private val chapterRepo: ChapterRepo,
  private val contentRepo: BookContentRepo,
) {

  fun root(): MediaItem = MediaItem(
    title = application.getString(R.string.media_session_root),
    folderType = FOLDER_TYPE_MIXED,
    isPlayable = false,
    mediaId = MediaId.Root,
  )

  suspend fun item(id: String): MediaItem? {
    val mediaId = id.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      MediaId.Root -> root()
      is MediaId.Book -> {
        bookRepository.get(mediaId.id)?.toMediaItem()
      }

      is MediaId.Chapter -> {
        val content = contentRepo.get(mediaId.bookId) ?: return null
        chapterRepo.get(mediaId.chapterId)?.toMediaItem(
          content = content,
        )
      }
    }
  }

  suspend fun chapters(bookId: BookId): List<MediaItem>? {
    val book = bookRepository.get(bookId) ?: return null
    return book.chapters.map { chapter ->
      chapter.toMediaItem(
        content = book.content,
      )
    }
  }

  suspend fun children(id: String): List<MediaItem>? {
    val mediaId = id.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      MediaId.Root -> {
        bookRepository.all()
          .map { book ->
            book.toMediaItem()
          }
      }

      is MediaId.Book -> {
        chapters(mediaId.id)
      }

      is MediaId.Chapter -> null
    }
  }
}

internal fun Book.toMediaItem() = MediaItem(
  title = content.name,
  mediaId = MediaId.Book(id),
  folderType = FOLDER_TYPE_TITLES,
  isPlayable = true,
  imageUri = content.cover?.toUri(),
)

fun Chapter.toMediaItem(
  content: BookContent,
) = MediaItem(
  title = name ?: id.value,
  mediaId = MediaId.Chapter(bookId = content.id, chapterId = id),
  folderType = FOLDER_TYPE_NONE,
  isPlayable = true,
  sourceUri = id.toUri(),
  imageUri = content.cover?.toUri(),
  artist = content.author,
  extras = Bundle().apply {
    putParcelableArray(EXTRA_CHAPTER_MARKS, chapterMarks.toTypedArray())
  },
)

internal fun MediaItem.chapterMarks(): List<ChapterMark> {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    mediaMetadata.extras!!.getParcelableArray(EXTRA_CHAPTER_MARKS, ChapterMark::class.java)!!
  } else {
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    mediaMetadata.extras!!.getParcelableArray(EXTRA_CHAPTER_MARKS) as Array<ChapterMark>
  }.toList()
}

private const val EXTRA_CHAPTER_MARKS = "chapterMarks"

fun String.toMediaIdOrNull(): MediaId? =
  try {
    Json.decodeFromString(MediaId.serializer(), this)
  } catch (e: SerializationException) {
    null
  }

@Serializable
sealed interface MediaId {
  @Serializable
  @SerialName("root")
  object Root : MediaId

  @Serializable
  @SerialName("book")
  data class Book(val id: BookId) : MediaId

  @Serializable
  @SerialName("chapter")
  data class Chapter(
    val bookId: BookId,
    val chapterId: ChapterId,
  ) : MediaId
}

private fun MediaItem(
  title: String,
  mediaId: MediaId,
  isPlayable: Boolean,
  @MediaMetadata.FolderType folderType: Int,
  album: String? = null,
  artist: String? = null,
  genre: String? = null,
  sourceUri: Uri? = null,
  imageUri: Uri? = null,
  extras: Bundle = Bundle.EMPTY,
): MediaItem {
  val metadata =
    MediaMetadata.Builder()
      .setAlbumTitle(album)
      .setTitle(title)
      .setArtist(artist)
      .setGenre(genre)
      .setFolderType(folderType)
      .setIsPlayable(isPlayable)
      .setArtworkUri(imageUri)
      .setExtras(extras)
      .build()

  return MediaItem.Builder()
    .setMediaId(Json.encodeToString(MediaId.serializer(), mediaId))
    .setMediaMetadata(metadata)
    .setUri(sourceUri)
    .build()
}
