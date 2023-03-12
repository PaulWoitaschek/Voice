package voice.playback.session

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import voice.common.BookId
import voice.common.pref.CurrentBook
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
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
) {

  fun root(): MediaItem = MediaItem(
    title = application.getString(R.string.media_session_root),
    browsable = true,
    isPlayable = false,
    mediaId = MediaId.Root,
    mediaType = MediaType.AudioBookRoot,
  )

  fun recent(): MediaItem? = MediaItem(
    title = application.getString(R.string.media_session_recent),
    browsable = true,
    isPlayable = false,
    mediaId = MediaId.Recent,
    mediaType = MediaType.AudioBook,
  ).takeIf { runBlocking { currentBookId.data.first() != null } }

  suspend fun item(id: String): MediaItem? {
    val mediaId = id.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      MediaId.Root -> root()
      is MediaId.Book -> {
        bookRepository.get(mediaId.id)?.toMediaItem()
      }

      is MediaId.Chapter -> {
        val content = contentRepo.get(mediaId.bookId) ?: return null
        chapterRepo.get(mediaId.chapterId)?.let {
          mediaItem(it, content)
        }
      }
      MediaId.Recent -> recent()
    }
  }

  fun mediaItemsWithStartPosition(book: Book): MediaItemsWithStartPosition {
    val items = book.chapters.map { chapter ->
      mediaItem(chapter = chapter, content = book.content)
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
      is MediaId.Chapter, MediaId.Root, MediaId.Recent, null -> null
    }
  }

  suspend fun chapters(bookId: BookId): List<MediaItem>? {
    val book = bookRepository.get(bookId) ?: return null
    return book.chapters.map { chapter ->
      mediaItem(
        chapter = chapter,
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
      MediaId.Recent -> {
        val bookId = currentBookId.data.first() ?: return null
        val book = bookRepository.get(bookId) ?: return null
        listOf(book.toMediaItem())
      }
    }
  }

  private fun Book.toMediaItem() = MediaItem(
    title = content.name,
    mediaId = MediaId.Book(id),
    browsable = false,
    isPlayable = true,
    imageUri = content.cover?.toProvidedUri(),
    mediaType = MediaType.AudioBook,
  )

  fun mediaItem(
    chapter: Chapter,
    content: BookContent,
  ) = MediaItem(
    title = chapter.name ?: chapter.id.value,
    mediaId = MediaId.Chapter(bookId = content.id, chapterId = chapter.id),
    browsable = false,
    isPlayable = true,
    sourceUri = chapter.id.toUri(),
    imageUri = content.cover?.toUri(),
    artist = content.author,
    mediaType = MediaType.AudioBookChapter,
    extras = Bundle().apply {
      putString(
        EXTRA_CHAPTER_MARKS,
        Json.encodeToString(ListSerializer(ChapterMark.serializer()), chapter.chapterMarks),
      )
    },
  )

  private fun File.toProvidedUri(): Uri = imageFileProvider.uri(this)
}

internal fun MediaItem.chapterMarks(): List<ChapterMark> {
  return Json.decodeFromString(
    deserializer = ListSerializer(ChapterMark.serializer()),
    string = mediaMetadata.extras!!.getString(EXTRA_CHAPTER_MARKS)!!,
  )
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

  @Serializable
  @SerialName("recent")
  object Recent : MediaId
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
