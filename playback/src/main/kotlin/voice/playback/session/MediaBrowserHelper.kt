package voice.playback.session

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import dagger.Reusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import voice.common.AppInfoProvider
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.BookComparator
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import voice.playback.R
import java.io.File
import javax.inject.Inject

@Reusable
class MediaBrowserHelper
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBookId: DataStore<BookId?>,
  private val context: Context,
  private val appInfoProvider: AppInfoProvider,
) {

  fun root(): String = bookUriConverter.allBooksId()

  suspend fun loadChildren(parentId: String): List<MediaBrowserCompat.MediaItem>? {
    val items = mediaItems(parentId)
    Logger.v("sending result $items")
    return items
  }

  private suspend fun mediaItems(parentId: String): List<MediaBrowserCompat.MediaItem>? {
    val type = bookUriConverter.parse(parentId)
      ?: return null
    return when (type) {
      is BookUriConverter.Parsed.AllBooks -> {
        val allBooks = repo.all()
          .sortedWith(BookComparator.ByLastPlayed)
        val currentBookId = currentBookId.data.first()
        val currentBook = allBooks.find { it.id == currentBookId }
        if (currentBook == null) {
          allBooks.map { it.toMediaDescription() }
        } else {
          // do NOT return the current book twice as this will break the listing due to stable IDs
          val booksWithoutCurrent = allBooks
            .filter { it != currentBook }
            .map { it.toMediaDescription() }

          listOf(currentBook.toMediaDescription(currentBookTitlePrefix())) + booksWithoutCurrent
        }
      }
      is BookUriConverter.Parsed.Book,
      is BookUriConverter.Parsed.Chapter,
      -> {
        Logger.w("Didn't handle $parentId")
        null
      }
    }
  }

  private fun currentBookTitlePrefix() = "${context.getString(R.string.current_book)}: "

  private suspend fun Book.toMediaDescription(
    titlePrefix: String = "",
  ): MediaBrowserCompat.MediaItem {
    val iconUri = content.cover?.let { fileProviderUri(it) }
    val mediaId = bookUriConverter.book(id)
    val description = MediaDescriptionCompat.Builder()
      .setTitle(titlePrefix + content.name)
      .setMediaId(mediaId)
      .setIconUri(iconUri)
      .build()
    return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
  }

  private suspend fun fileProviderUri(coverFile: File): Uri? {
    return withContext(Dispatchers.IO) {
      if (coverFile.exists()) {
        FileProvider.getUriForFile(context, "${appInfoProvider.applicationID}.coverprovider", coverFile)
          .apply {
            context.grantUriPermission(
              "com.google.android.wearable.app",
              this,
              Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            context.grantUriPermission(
              "com.google.android.projection.gearhead",
              this,
              Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
          }
      } else {
        null
      }
    }
  }
}
