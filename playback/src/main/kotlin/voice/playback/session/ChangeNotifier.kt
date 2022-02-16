package voice.playback.session

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import voice.data.Book
import voice.data.Chapter
import voice.data.toUri
import voice.playback.R
import voice.playback.androidauto.AndroidAutoConnectedReceiver
import voice.playback.di.PlaybackScope
import javax.inject.Inject

/**
 * Sets updated metadata on the media session and sends broadcasts about meta changes
 */
@PlaybackScope
class ChangeNotifier
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  private val mediaSession: MediaSessionCompat,
  private val context: Context,
  private val autoConnectedReceiver: AndroidAutoConnectedReceiver
) {

  /** The last file the [.notifyChange] has used to update the metadata. **/
  @Volatile
  private var lastFileForMetaData: Uri? = null

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

  fun updatePlaybackState(@PlaybackStateCompat.State state: Int, book: Book?) {
    val builder = if (autoConnectedReceiver.connected) {
      playbackStateBuilderForAuto
    } else {
      playbackStateBuilder
    }
    val playbackState = builder
      .apply {
        setState(
          state,
          book?.content?.positionInChapter ?: PLAYBACK_POSITION_UNKNOWN,
          book?.content?.playbackSpeed ?: 1F
        )

        if (book != null) {
          setActiveQueueItemId(book.chapters.indexOf(book.currentChapter).toLong())
        }
      }
      .build()
    mediaSession.setPlaybackState(playbackState)
  }

  suspend fun updateMetadata(book: Book) {
    val content = book.content
    val currentChapter = book.currentChapter

    val bookName = content.name
    val chapterName = currentChapter.name
    val author = content.author

    if (lastFileForMetaData != content.currentChapter.toUri()) {
      appendQueue(book)
      val cover = context.imageLoader
        .execute(ImageRequest.Builder(context)
          .data(content.cover)
          .size(width = context.resources.getDimensionPixelSize(R.dimen.compat_notification_large_icon_max_width),
            height = context.resources.getDimensionPixelSize(R.dimen.compat_notification_large_icon_max_height))
          .fallback(R.drawable.album_art)
          .error(R.drawable.album_art)
          .allowHardware(false)
          .build()
        )
        .drawable!!.toBitmap()
      val mediaMetaData = MediaMetadataCompat.Builder()
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cover)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentChapter.duration)
        .putLong(
          MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
          (content.currentChapterIndex + 1).toLong()
        )
        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, book.chapters.size.toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, bookName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, author)
        .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, author)
        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook")
        .build()
      mediaSession.setMetadata(mediaMetaData)

      lastFileForMetaData = content.currentChapter.toUri()
    }
  }

  private fun appendQueue(book: Book) {
    val queue = book.chapters.mapIndexed { index, chapter ->
      MediaSessionCompat.QueueItem(chapter.toMediaDescription(book), index.toLong())
    }

    if (queue.isNotEmpty()) {
      mediaSession.setQueue(queue)
      mediaSession.setQueueTitle(book.content.name)
    }
  }

  private fun Chapter.toMediaDescription(book: Book): MediaDescriptionCompat {
    return MediaDescriptionCompat.Builder()
      .setTitle(name)
      .setMediaId(bookUriConverter.chapter(book.id, id))
      .build()
  }
}
