package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.FileProvider
import androidx.media.MediaBrowserServiceCompat
import dagger.Reusable
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.persistence.pref.Pref
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
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
  private val context: Context
) {

  fun onGetRoot(): MediaBrowserServiceCompat.BrowserRoot = MediaBrowserServiceCompat.BrowserRoot(
    bookUriConverter.allBooks().toString(),
    null
  )

  fun onLoadChildren(
    parentId: String,
    result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
  ) {
    Timber.d("onLoadChildren $parentId, $result")
    result.detach()
    GlobalScope.launch {
      val uri = Uri.parse(parentId)
      val items = mediaItems(uri)
      Timber.d("sending result $items")
      result.sendResult(items)
    }
  }

  private suspend fun mediaItems(uri: Uri): List<MediaBrowserCompat.MediaItem>? {
    val type = bookUriConverter.type(uri)

    if (type == BookUriConverter.ROOT) {
      val currentBook = repo.bookById(currentBookIdPref.value)
      val current = currentBook?.toMediaDescription(
        titlePrefix = "${context.getString(R.string.current_book)}: "
      )

      // do NOT return the current book twice as this will break the listing due to stable IDs
      val all = repo.activeBooks
        .filter { it != currentBook }
        .map { it.toMediaDescription() }

      return if (current == null) {
        all
      } else {
        listOf(current) + all
      }
    } else {
      return null
    }
  }

  private suspend fun Book.toMediaDescription(titlePrefix: String = ""): MediaBrowserCompat.MediaItem {
    val iconUri = fileProviderUri(coverFile())
    val mediaId = bookUriConverter.book(id).toString()
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
        FileProvider.getUriForFile(context, "de.ph1b.audiobook.coverprovider", coverFile)
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
