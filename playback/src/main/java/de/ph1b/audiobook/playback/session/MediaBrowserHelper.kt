package de.ph1b.audiobook.playback.session

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.FileProvider
import dagger.Reusable
import de.ph1b.audiobook.common.ApplicationIdProvider
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.R
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Helper class for MediaBrowserServiceCompat handling
 */
@Reusable
class MediaBrowserHelper
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
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
      val currentBook = repo.bookByIdBlocking(currentBookIdPref.value)
      val current = currentBook?.toMediaDescription(
        titlePrefix = currentBookTitlePrefix()
      )

      // do NOT return the current book twice as this will break the listing due to stable IDs
      val all = repo.activeBooks()
        .filter { it != currentBook }
        .sortedWith(BookComparator.BY_LAST_PLAYED)
        .map { it.toMediaDescription() }

      return if (current == null) {
        all
      } else {
        listOf(current) + all
      }
    } else {
      Timber.e("Didn't handle $parentId")
      return null
    }
  }

  private fun currentBookTitlePrefix() = "${context.getString(R.string.current_book)}: "

  private suspend fun Book.toMediaDescription(
    titlePrefix: String = ""
  ): MediaBrowserCompat.MediaItem {
    val iconUri = fileProviderUri(coverFile(context))
    val mediaId = bookUriConverter.bookId(id)
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
