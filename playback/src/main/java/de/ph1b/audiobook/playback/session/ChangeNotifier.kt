package de.ph1b.audiobook.playback.session

import android.content.Context
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.common.CoverReplacement
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.playback.R
import de.ph1b.audiobook.playback.androidauto.AndroidAutoConnectedReceiver
import de.ph1b.audiobook.playback.di.PlaybackScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Sets updated metadata on the media session and sends broadcasts about meta changes
 */
@PlaybackScope
class ChangeNotifier
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  private val mediaSession: MediaSessionCompat,
  private val imageHelper: ImageHelper,
  private val context: Context,
  private val autoConnectedReceiver: AndroidAutoConnectedReceiver
) {

  /** The last file the [.notifyChange] has used to update the metadata. **/
  @Volatile
  private var lastFileForMetaData = File("")

  private val playbackStateBuilder = PlaybackStateCompat.Builder()
    .setActions(
      PlaybackStateCompat.ACTION_FAST_FORWARD or
        PlaybackStateCompat.ACTION_PAUSE or
        PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
        PlaybackStateCompat.ACTION_PLAY_PAUSE or
        PlaybackStateCompat.ACTION_REWIND or
        PlaybackStateCompat.ACTION_SEEK_TO or
        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
        PlaybackStateCompat.ACTION_STOP
    )

  // use a different feature set for Android Auto
  private val playbackStateBuilderForAuto = PlaybackStateCompat.Builder()
    .setActions(
      PlaybackStateCompat.ACTION_PAUSE or
        PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
        PlaybackStateCompat.ACTION_PLAY_PAUSE or
        PlaybackStateCompat.ACTION_SEEK_TO or
        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
        PlaybackStateCompat.ACTION_STOP
    )
    .addCustomAction(
      ANDROID_AUTO_ACTION_REWIND,
      context.getString(R.string.rewind),
      R.drawable.ic_fast_rewind
    )
    .addCustomAction(
      ANDROID_AUTO_ACTION_FAST_FORWARD,
      context.getString(R.string.fast_forward),
      R.drawable.ic_fast_forward
    )
    .addCustomAction(
      ANDROID_AUTO_ACTION_PREVIOUS,
      context.getString(R.string.previous_track),
      R.drawable.ic_skip_previous
    )
    .addCustomAction(
      ANDROID_AUTO_ACTION_NEXT,
      context.getString(R.string.next_track),
      R.drawable.ic_skip_next
    )

  fun updatePlaybackState(@PlaybackStateCompat.State state: Int, content: BookContent?) {
    val playbackState = (if (autoConnectedReceiver.connected) playbackStateBuilderForAuto else playbackStateBuilder)
      .setState(state, content?.positionInChapter ?: PLAYBACK_POSITION_UNKNOWN, content?.playbackSpeed ?: 1F)
      .apply {
        if (content != null) {
          setActiveQueueItemId(content.chapters.indexOf(content.currentChapter).toLong())
        }
      }
      .build()
    mediaSession.setPlaybackState(playbackState)
  }

  suspend fun updateMetadata(book: Book) {
    val currentChapter = book.content.currentChapter

    val bookName = book.name
    val chapterName = currentChapter.name
    val author = book.author

    if (lastFileForMetaData != book.content.currentFile) {
      appendQueue(book)
      // this check is necessary. Else the lockscreen controls will flicker due to
      // an updated picture
      var bitmap = withContext(IO) {
        val coverFile = book.coverFile(context)
        if (coverFile.exists() && coverFile.canRead()) {
          try {
            Picasso.get()
              .load(coverFile)
              .get()
              .run {
                // we make a copy because we do not want to use picassos bitmap, since
                // MediaSessionCompat recycles our bitmap eventually which would make
                // picassos cached bitmap useless.
                copy(config, false)
              }
          } catch (e: IOException) {
            Timber.e(e)
            null
          }
        } else {
          null
        }
      }

      if (bitmap == null) {
        val replacement = CoverReplacement(book.name, context)
        bitmap = imageHelper.drawableToBitmap(
          replacement,
          imageHelper.smallerScreenSize,
          imageHelper.smallerScreenSize
        )
      }

      val mediaMetaData = MediaMetadataCompat.Builder()
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentChapter.duration)
        .putLong(
          MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
          (book.content.currentChapterIndex + 1).toLong()
        )
        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, book.content.chapters.size.toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, bookName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, author)
        .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, author)
        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook")
        .build()
      mediaSession.setMetadata(mediaMetaData)

      lastFileForMetaData = book.content.currentFile
    }
  }

  private fun appendQueue(book: Book) {
    val queue = book.content.chapters.mapIndexed { index, chapter ->
      MediaSessionCompat.QueueItem(chapter.toMediaDescription(book), index.toLong())
    }

    if (queue.isNotEmpty()) {
      mediaSession.setQueue(queue)
      mediaSession.setQueueTitle(book.name)
    }
  }

  private fun Chapter.toMediaDescription(book: Book): MediaDescriptionCompat {
    return MediaDescriptionCompat.Builder()
      .setTitle(name)
      .setMediaId(bookUriConverter.chapterId(book.id, id))
      .build()
  }
}
