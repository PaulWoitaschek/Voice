package de.ph1b.audiobook.playback.session

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import dagger.Reusable
import de.ph1b.audiobook.common.ApplicationIdProvider
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.playback.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Helper class for MediaBrowserServiceCompat handling
 */
@Reusable
class MediaBrowserHelper
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  private val repo: BookRepo2,
  @CurrentBook
  private val currentBookId: DataStore<Uri?>,
  private val context: Context,
  private val applicationIdProvider: ApplicationIdProvider
) {

  fun root(): String = bookUriConverter.allBooksId()

  suspend fun loadChildren(parentId: String): List<MediaBrowserCompat.MediaItem>? {
    val items = mediaItems(parentId)
    Timber.d("sending result $items")
    return items
  }

  private suspend fun mediaItems(parentId: String): List<MediaBrowserCompat.MediaItem>? {
    val type = bookUriConverter.parse(parentId)
      ?: return null
    if (type is BookUriConverter.Parsed.AllBooks) {
      val allBooks = repo.flow().first()
        .sortedWith(BookComparator.BY_LAST_PLAYED)
      val currentBookId = currentBookId.data.first()
      val currentBook = allBooks.find { it.uri == currentBookId }
      return if (currentBook == null) {
        allBooks.map { it.toMediaDescription() }
      } else {
        // do NOT return the current book twice as this will break the listing due to stable IDs
        val booksWithoutCurrent = allBooks
          .filter { it != currentBook }
          .map { it.toMediaDescription() }

        listOf(currentBook.toMediaDescription(currentBookTitlePrefix())) + booksWithoutCurrent
      }
    } else {
      Timber.e("Didn't handle $parentId")
      return null
    }
  }

  private fun currentBookTitlePrefix() = "${context.getString(R.string.current_book)}: "

  private suspend fun BookContent2.toMediaDescription(
    titlePrefix: String = ""
  ): MediaBrowserCompat.MediaItem {
    val iconUri = cover?.let { fileProviderUri(it) }
    val mediaId = bookUriConverter.bookId(uri)
    val description = MediaDescriptionCompat.Builder()
      .setTitle(titlePrefix + name)
      .setMediaId(mediaId)
      .setIconUri(iconUri)
      .build()
    return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
  }

  private suspend fun fileProviderUri(coverFile: File): Uri? {
    return withContext(Dispatchers.IO) {
      if (coverFile.exists()) {
        FileProvider.getUriForFile(context, "${applicationIdProvider.applicationID}.coverprovider", coverFile)
          .apply {
            context.grantUriPermission(
              "com.google.android.wearable.app",
              this,
              Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.grantUriPermission(
              "com.google.android.projection.gearhead",
              this,
              Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
          }
      } else null
    }
  }
}
