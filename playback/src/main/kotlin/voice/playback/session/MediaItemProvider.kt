package voice.playback.session

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
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
import java.io.File
import javax.inject.Inject

class MediaItemProvider
@Inject constructor(
  private val bookRepository: BookRepository,
  private val application: Application,
  private val chapterRepo: ChapterRepo,
  private val contentRepo: BookContentRepo,
  private val imageFileProvider: ImageFileProvider,
) {

  fun root(): MediaItem = MediaItem(
    title = application.getString(R.string.media_session_root),
    browsable = true,
    isPlayable = false,
    mediaId = MediaId.Root,
    mediaType = MediaType.AudioBook,
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

  fun mediaItemsWithStartPosition(book: Book): MediaItemsWithStartPosition {
    val items = book.chapters.map { chapter ->
      chapter.toMediaItem(
        content = book.content,
      )
    }
    return MediaItemsWithStartPosition(
      items,
      book.content.currentChapterIndex,
      book.content.positionInChapter,
    )
  }

  suspend fun mediaItemsWithStartPosition(id: String): MediaItemsWithStartPosition? {
    return when (val mediaId = id.toMediaIdOrNull()) {
      is MediaId.Book -> {
        val book = bookRepository.get(mediaId.id) ?: return null
        mediaItemsWithStartPosition(book)
      }
      is MediaId.Chapter, MediaId.Root, null -> null
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

  private fun Book.toMediaItem() = MediaItem(
    title = content.name,
    mediaId = MediaId.Book(id),
    browsable = true,
    isPlayable = true,
    imageUri = content.cover?.toProvidedUri(),
    mediaType = MediaType.AudioBook,
  )

  private fun File.toProvidedUri(): Uri = imageFileProvider.uri(this)
}

fun Chapter.toMediaItem(
  content: BookContent,
) = MediaItem(
  title = name ?: id.value,
  mediaId = MediaId.Chapter(bookId = content.id, chapterId = id),
  browsable = false,
  isPlayable = true,
  sourceUri = id.toUri(),
  imageUri = content.cover?.toUri(),
  artist = content.author,
  mediaType = MediaType.AudioBookChapter,
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

private enum class MediaType {
  AudioBook, AudioBookChapter, AudioBookRoot
}

private fun MediaItem(
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
