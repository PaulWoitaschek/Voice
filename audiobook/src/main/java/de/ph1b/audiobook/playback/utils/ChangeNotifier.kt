package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.squareup.picasso.Picasso
import d
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.blocking
import java.io.File
import javax.inject.Inject

/**
 * Sets updated metadata on the media session and sends broadcasts about meta changes
 *
 * @author Paul Woitaschek
 */
class ChangeNotifier(private val mediaSession: MediaSessionCompat) {

  init {
    App.component().inject(this)
  }

  @Inject lateinit var imageHelper: ImageHelper
  @Inject lateinit var context: Context
  @Inject lateinit var playStateManager: PlayStateManager

  /** The last file the [.notifyChange] has used to update the metadata. **/
  @Volatile private var lastFileForMetaData = File("")

  private val playbackStateBuilder = PlaybackStateCompat.Builder()
    .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
      PlaybackStateCompat.ACTION_REWIND or
      PlaybackStateCompat.ACTION_PLAY or
      PlaybackStateCompat.ACTION_PAUSE or
      PlaybackStateCompat.ACTION_PLAY_PAUSE or
      PlaybackStateCompat.ACTION_STOP or
      PlaybackStateCompat.ACTION_FAST_FORWARD or
      PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
      PlaybackStateCompat.ACTION_SEEK_TO)

  private val mediaMetaDataBuilder = MediaMetadataCompat.Builder()

  fun notify(what: Type, book: Book) {
    d { "updateRemoteControlClient called" }

    val c = book.currentChapter()
    val playState = playStateManager.playState.value

    val bookName = book.name
    val chapterName = c.name
    val author = book.author
    val position = book.time

    context.sendBroadcast(what.broadcastIntent(author, bookName, chapterName, playState, position))

    //noinspection ResourceType
    playbackStateBuilder.setState(playState.playbackStateCompat, position.toLong(), book.playbackSpeed)
    mediaSession.setPlaybackState(playbackStateBuilder.build())

    if (what == Type.METADATA && lastFileForMetaData != book.currentFile) {
      // this check is necessary. Else the lockscreen controls will flicker due to
      // an updated picture
      var bitmap: Bitmap? = null
      val coverFile = book.coverFile()
      if (coverFile.exists() && coverFile.canRead()) {
        bitmap = Picasso.with(context)
          .blocking { load(coverFile).get() }
      }
      if (bitmap == null) {
        val replacement = CoverReplacement(book.name, context)
        d { "replacement dimen: ${replacement.intrinsicWidth}:${replacement.intrinsicHeight}" }
        bitmap = imageHelper.drawableToBitmap(replacement, imageHelper.smallerScreenSize, imageHelper.smallerScreenSize)
      }
      // we make a copy because we do not want to use picassos bitmap, since
      // MediaSessionCompat recycles our bitmap eventually which would make
      // picassos cached bitmap useless.
      bitmap = bitmap.copy(bitmap.config, true)
      mediaMetaDataBuilder
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, c.duration.toLong())
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (book.chapters.indexOf(book.currentChapter()) + 1).toLong())
        .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, book.chapters.size.toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, bookName)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
        .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, author)
        .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, author)
        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook")
      mediaSession.setMetadata(mediaMetaDataBuilder.build())

      lastFileForMetaData = book.currentFile
    }
  }

  enum class Type(private val intentUrl: String) {
    METADATA("com.android.music.metachanged"),
    PLAY_STATE("com.android.music.playstatechange");

    fun broadcastIntent(author: String?,
                        bookName: String,
                        chapterName: String,
                        playState: PlayStateManager.PlayState,
                        time: Int) =
      Intent(intentUrl).apply {
        putExtra("id", 1)
        if (author != null) {
          putExtra("artist", author)
        }
        putExtra("album", bookName)
        putExtra("track", chapterName)
        putExtra("playing", playState === PlayStateManager.PlayState.PLAYING)
        putExtra("position", time)
      }
  }
}
