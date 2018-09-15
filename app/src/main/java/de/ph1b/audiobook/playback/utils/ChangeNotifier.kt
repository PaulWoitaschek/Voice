package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.playback.ANDROID_AUTO_ACTION_FAST_FORWARD
import de.ph1b.audiobook.playback.ANDROID_AUTO_ACTION_NEXT
import de.ph1b.audiobook.playback.ANDROID_AUTO_ACTION_PREVIOUS
import de.ph1b.audiobook.playback.ANDROID_AUTO_ACTION_REWIND
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.blocking
import java.io.File

/**
 * Sets updated metadata on the media session and sends broadcasts about meta changes
 */
class ChangeNotifier(
  private val bookUriConverter: BookUriConverter,
  private val mediaSession: MediaSessionCompat,
  private val imageHelper: ImageHelper,
  private val context: Context,
  private val playStateManager: PlayStateManager
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

  //use a different feature set for Android Auto
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

  private val mediaMetaDataBuilder = MediaMetadataCompat.Builder()

  suspend fun notify(what: Type, book: Book, forAuto: Boolean = false) {
    val currentChapter = book.content.currentChapter
    val playState = playStateManager.playState

    val bookName = book.name
    val chapterName = currentChapter.name
    val author = book.author
    val position = book.content.positionInChapter

    context.sendBroadcast(what.broadcastIntent(author, bookName, chapterName, playState, position))

    val playbackState = (if (forAuto) playbackStateBuilderForAuto else playbackStateBuilder)
      .setState(playState.playbackStateCompat, position.toLong(), book.content.playbackSpeed)
      .setActiveQueueItemId(book.content.chapters.indexOf(book.content.currentChapter).toLong())
      .build()

    mediaSession.setPlaybackState(playbackState)

    if (what == Type.METADATA && lastFileForMetaData != book.content.currentFile) {
      appendQueue(book)
      // this check is necessary. Else the lockscreen controls will flicker due to
      // an updated picture
      var bitmap: Bitmap? = null
      val coverFile = book.coverFile()
      if (coverFile.exists() && coverFile.canRead()) {
        bitmap = Picasso.get()
          .blocking { load(coverFile).get() }
      }
      if (bitmap == null) {
        val replacement = CoverReplacement(book.name, context)
        bitmap = imageHelper.drawableToBitmap(
          replacement,
          imageHelper.smallerScreenSize,
          imageHelper.smallerScreenSize
        )
      }
      // we make a copy because we do not want to use picassos bitmap, since
      // MediaSessionCompat recycles our bitmap eventually which would make
      // picassos cached bitmap useless.
      bitmap = bitmap.copy(bitmap.config, true)
      mediaMetaDataBuilder
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentChapter.duration.toLong())
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
      mediaSession.setMetadata(mediaMetaDataBuilder.build())

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

  enum class Type(intentUrl: String) {
    METADATA("com.android.music.metachanged"),
    PLAY_STATE("com.android.music.playstatechange");

    private val intent = Intent(intentUrl)

    fun broadcastIntent(
      author: String?,
      bookName: String,
      chapterName: String,
      playState: PlayStateManager.PlayState,
      time: Int
    ) =
      intent.apply {
        putExtra("id", 1)
        if (author != null) {
          putExtra("artist", author)
        } else removeExtra("artist")
        putExtra("album", bookName)
        putExtra("track", chapterName)
        putExtra("playing", playState === PlayStateManager.PlayState.PLAYING)
        putExtra("position", time)
        putExtra("package", BuildConfig.APPLICATION_ID)
      }
  }

  private fun Chapter.toMediaDescription(book: Book): MediaDescriptionCompat {
    val index = book.content.chapters.indexOf(this)
    return MediaDescriptionCompat.Builder()
      .setTitle(name)
      .setMediaId(bookUriConverter.chapter(book.id, index).toString())
      .build()
  }
}
