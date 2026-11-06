package voice.core.playback.session

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE
import androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_STATUS
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
import androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import dev.zacsweers.metro.Inject
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookComparator
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.category
import voice.core.data.durationMs
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ChapterRepo
import voice.core.data.store.CurrentBookStore
import voice.core.data.toUri
import voice.core.strings.R as StringsR

@Inject
class MediaItemProvider(
  private val bookRepository: BookRepository,
  private val application: Application,
  private val chapterRepo: ChapterRepo,
  private val imageFileProvider: ImageFileProvider,
  private val currentBookStoreId: CurrentBookStore,
) {

  fun root(): MediaItem =
    MediaItem(
      title = application.getString(StringsR.string.media_session_library_root),
      browsable = true,
      isPlayable = false,
      mediaId = MediaId.Root,
      mediaType = MediaType.AudioBookFolder,
    )

  // Blocking on purpose: legacy browsers get the library root on the main thread and wait for it
  // there, so a
  // coroutine that resumes on the main thread would never finish.
  fun recent(): MediaItem? =
    MediaItem(
        title = application.getString(StringsR.string.media_session_library_recent),
        browsable = true,
        isPlayable = false,
        mediaId = MediaId.Recent,
        mediaType = MediaType.AudioBook,
      )
      .takeIf { runBlocking { currentBookStoreId.data.first() != null } }

  suspend fun item(id: String): MediaItem? {
    val mediaId = id.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      MediaId.Root -> root()
      is MediaId.Book -> {
        bookRepository.get(mediaId.id)?.let(::mediaItem)
      }
      is MediaId.Chapter -> {
        val book = bookRepository.get(mediaId.bookId) ?: return null
        chapterRepo.get(mediaId.chapterId)?.let {
          mediaItem(it, book)
        }
      }
      is MediaId.ChapterMark -> {
        val book = bookRepository.get(mediaId.bookId) ?: return null
        val chapter = chapterRepo.get(mediaId.chapterId) ?: return null
        val mark = chapter.chapterMarks.getOrNull(mediaId.markIndex) ?: return null
        mediaItem(
          playbackItem =
            PlaybackItem(
              index = 0,
              bookId = mediaId.bookId,
              chapter = chapter,
              markIndex = mediaId.markIndex,
              mark = mark,
            ),
          book = book,
        )
      }
      MediaId.Recent -> recent()
      is MediaId.Category -> mediaItem(mediaId.category)
    }
  }

  fun mediaItemsWithStartPosition(book: Book): MediaItemsWithStartPosition {
    val startItem =
      book.playbackItemForPosition(
        chapterId = book.content.currentChapter,
        positionInChapterMs = book.content.positionInChapter,
      )
    return MediaItemsWithStartPosition(
      playbackItems(book),
      startItem?.index ?: C.INDEX_UNSET,
      startItem?.positionInMediaItem(book.content.positionInChapter) ?: C.TIME_UNSET,
    )
  }

  suspend fun mediaItemsWithStartPosition(id: String): MediaItemsWithStartPosition? {
    return when (val mediaId = id.toMediaIdOrNull()) {
      is MediaId.Book -> {
        val book = bookRepository.get(mediaId.id) ?: return null
        mediaItemsWithStartPosition(book)
      }
      is MediaId.Chapter,
      is MediaId.ChapterMark,
      is MediaId.Category,
      MediaId.Root,
      MediaId.Recent,
      null -> null
    }
  }

  suspend fun chapters(bookId: BookId): List<MediaItem>? {
    val book = bookRepository.get(bookId) ?: return null
    return playbackItems(book)
  }

  internal fun chapterMediaItems(book: Book): List<MediaItem> {
    return book.chapters.map { chapter ->
      mediaItem(chapter, book)
    }
  }

  internal fun playbackItems(book: Book): List<MediaItem> {
    return book.playbackItems().map { playbackItem ->
      mediaItem(playbackItem, book)
    }
  }

  suspend fun children(id: String): List<MediaItem>? {
    val mediaId = id.toMediaIdOrNull() ?: return null
    return when (mediaId) {
      // Android Auto shows these as tabs
      MediaId.Root -> {
        val categories = libraryBooks().map { it.category }.toSet()
        BookCategory.entries.filter { it in categories }.map(::mediaItem)
      }
      is MediaId.Category -> {
        libraryBooks()
          .filter { it.category == mediaId.category }
          .sortedWith(mediaId.category.comparator)
          .map(::mediaItem)
      }
      is MediaId.Book -> chapters(mediaId.id)
      is MediaId.Chapter,
      is MediaId.ChapterMark -> null
      MediaId.Recent -> {
        val bookId = currentBookStoreId.data.first() ?: return null
        val book = bookRepository.get(bookId) ?: return null
        listOf(mediaItem(book))
      }
    }
  }

  private suspend fun libraryBooks(): List<Book> =
    bookRepository.all().filterNot { it.custom.hidden }

  fun mediaItem(book: Book): MediaItem =
    MediaItem(
      title = book.name,
      mediaId = MediaId.Book(book.id),
      browsable = false,
      isPlayable = true,
      imageUri = book.cover?.toProvidedUri(),
      artist = book.author,
      subtitle = book.subtitle(),
      mediaType = MediaType.AudioBook,
      extras = book.completionExtras(),
    )

  // Android Auto doesn't always draw the progress bar
  private fun Book.subtitle(): String? {
    if (category != BookCategory.CURRENT) return null
    val remainingSeconds = ((duration - position) / content.playbackSpeed / 1000).toLong()
    val remaining =
      application.getString(
        StringsR.string.playback_book_progress_remaining,
        DateUtils.formatElapsedTime(remainingSeconds),
      )
    return listOfNotNull(author, remaining).joinToString(" · ")
  }

  private fun mediaItem(category: BookCategory): MediaItem =
    MediaItem(
      title = application.getString(category.title),
      mediaId = MediaId.Category(category),
      browsable = true,
      isPlayable = false,
      mediaType = MediaType.AudioBookFolder,
    )

  private fun mediaItem(
    chapter: Chapter,
    book: Book,
  ) =
    MediaItem(
      title = chapter.name ?: chapter.id.value,
      mediaId = MediaId.Chapter(bookId = book.id, chapterId = chapter.id),
      browsable = false,
      isPlayable = true,
      sourceUri = chapter.id.toUri(),
      imageUri = book.cover?.toProvidedUri(),
      artist = book.author,
      mediaType = MediaType.AudioBookChapter,
    )

  private fun mediaItem(
    playbackItem: PlaybackItem,
    book: Book,
  ) =
    MediaItem(
      title = playbackItem.mark.name ?: playbackItem.chapter.name ?: playbackItem.chapter.id.value,
      mediaId = playbackItem.mediaId,
      browsable = false,
      isPlayable = true,
      sourceUri = playbackItem.chapter.id.toUri(),
      imageUri = book.cover?.toProvidedUri(),
      artist = book.author,
      durationMs = playbackItem.mark.durationMs,
      mediaType = MediaType.AudioBookChapter,
    )

  private fun File.toProvidedUri(): Uri = imageFileProvider.uri(this)
}

// the same order as in the library
private val BookCategory.comparator: Comparator<Book>
  get() =
    when (this) {
      BookCategory.CURRENT,
      BookCategory.FINISHED -> BookComparator.ByLastPlayed
      BookCategory.NOT_STARTED -> BookComparator.ByName
    }

private val BookCategory.title: Int
  get() =
    when (this) {
      BookCategory.CURRENT -> StringsR.string.library_category_current_title
      BookCategory.NOT_STARTED -> StringsR.string.library_category_not_started_title
      BookCategory.FINISHED -> StringsR.string.library_category_completed_title
    }

private fun Book.completionExtras(): Bundle =
  Bundle().apply {
    when (category) {
      BookCategory.NOT_STARTED ->
        putInt(EXTRAS_KEY_COMPLETION_STATUS, EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED)
      BookCategory.CURRENT -> {
        putInt(EXTRAS_KEY_COMPLETION_STATUS, EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED)
        putDouble(EXTRAS_KEY_COMPLETION_PERCENTAGE, position.toDouble() / duration)
      }
      BookCategory.FINISHED ->
        putInt(EXTRAS_KEY_COMPLETION_STATUS, EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED)
    }
  }
