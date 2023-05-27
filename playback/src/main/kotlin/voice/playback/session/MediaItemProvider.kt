package voice.playback.session

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.toUri
import java.io.File
import javax.inject.Inject
import voice.strings.R as StringsR

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
    title = application.getString(StringsR.string.media_session_root),
    browsable = true,
    isPlayable = false,
    mediaId = MediaId.Root,
    mediaType = MediaType.AudioBookRoot,
  )

  fun recent(): MediaItem? = MediaItem(
    title = application.getString(StringsR.string.media_session_recent),
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
        bookRepository.get(mediaId.id)?.let(::mediaItem)
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
    return MediaItemsWithStartPosition(
      listOf(mediaItem(book)),
      C.INDEX_UNSET,
      C.TIME_UNSET,
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
    return chapters(book)
  }

  internal fun chapters(book: Book): List<MediaItem> {
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
            mediaItem(book)
          }
      }
      is MediaId.Book -> {
        chapters(mediaId.id)
      }
      is MediaId.Chapter -> null
      MediaId.Recent -> {
        val bookId = currentBookId.data.first() ?: return null
        val book = bookRepository.get(bookId) ?: return null
        listOf(mediaItem(book))
      }
    }
  }

  fun mediaItem(book: Book): MediaItem = MediaItem(
    title = book.content.name,
    mediaId = MediaId.Book(book.id),
    browsable = false,
    isPlayable = true,
    imageUri = book.content.cover?.toProvidedUri(),
    mediaType = MediaType.AudioBook,
  )

  private fun mediaItem(
    chapter: Chapter,
    content: BookContent,
  ) = MediaItem(
    title = chapter.name ?: chapter.id.value,
    mediaId = MediaId.Chapter(bookId = content.id, chapterId = chapter.id),
    browsable = false,
    isPlayable = true,
    sourceUri = chapter.id.toUri(),
    imageUri = content.cover?.toProvidedUri(),
    artist = content.author,
    mediaType = MediaType.AudioBookChapter,
  )

  private fun File.toProvidedUri(): Uri = imageFileProvider.uri(this)
}
