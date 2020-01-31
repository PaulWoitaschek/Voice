package de.ph1b.audiobook.playback.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_FAST_FORWARD
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_REWIND
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.common.CoverReplacement
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.common.MAX_IMAGE_SIZE
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.playback.R
import de.ph1b.audiobook.playback.di.PerService
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Provides Notifications based on playing information.
 */
@PerService
class NotificationCreator
@Inject constructor(
  private val context: Context,
  private val imageHelper: ImageHelper,
  private val playStateManager: PlayStateManager,
  private val mediaSession: MediaSessionCompat,
  notificationChannelCreator: NotificationChannelCreator,
  private val toBookIntentProvider: ToBookIntentProvider
) {

  private val fastForwardAction = NotificationCompat.Action(
    R.drawable.ic_fast_forward_white_36dp,
    context.getString(R.string.fast_forward),
    buildMediaButtonPendingIntent(context, ACTION_FAST_FORWARD)
  )

  private val rewindAction = NotificationCompat.Action(
    R.drawable.ic_rewind_white_36dp,
    context.getString(R.string.rewind),
    buildMediaButtonPendingIntent(context, ACTION_REWIND)
  )

  private val playAction = NotificationCompat.Action(
    R.drawable.ic_play_white_36dp,
    context.getString(R.string.play),
    buildMediaButtonPendingIntent(context, ACTION_PLAY)
  )

  private val pauseAction = NotificationCompat.Action(
    R.drawable.ic_pause_white_36dp,
    context.getString(R.string.pause),
    buildMediaButtonPendingIntent(context, ACTION_PAUSE)
  )

  init {
    notificationChannelCreator.createChannel()
  }

  private var cachedImage: CachedImage? = null

  suspend fun createNotification(book: Book): Notification {
    val mediaStyle = MediaStyle()
      .setShowActionsInCompactView(0, 1, 2)
      .setCancelButtonIntent(stopIntent())
      .setShowCancelButton(true)
      .setMediaSession(mediaSession.sessionToken)
    return Builder(context, MUSIC_CHANNEL_ID)
      .addAction(rewindAction)
      .addPlayPauseAction(playStateManager.playState)
      .addAction(fastForwardAction)
      .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
      .setChapterInfo(book)
      .setContentIntent(contentIntent(book))
      .setContentTitle(book)
      .setDeleteIntent(stopIntent())
      .setLargeIcon(book)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setShowWhen(false)
      .setSmallIcon(R.drawable.ic_notification)
      .setStyle(mediaStyle)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setWhen(0)
      .build()
  }

  private suspend fun cover(book: Book): Bitmap {
    // first try to get use a cached image
    cachedImage?.let {
      if (it.matches(book)) return it.cover
    }

    val width = imageHelper.smallerScreenSize
    val height = imageHelper.smallerScreenSize

    // get the cover or fallback to a replacement
    val coverFile = book.coverFile(context)
    val picassoCover = withContext(Dispatchers.IO) {
      if (coverFile.canRead() && coverFile.length() < MAX_IMAGE_SIZE) {
        try {
          Picasso.get()
            .load(coverFile)
            .resize(width, height)
            .get()
        } catch (e: IOException) {
          Timber.e(e, "Can't decode $coverFile")
          null
        }
      } else null
    }
    val cover = picassoCover ?: imageHelper.drawableToBitmap(
      CoverReplacement(book.name, context),
      width,
      height
    )

    // add a cache entry
    cachedImage = CachedImage(book.id, cover)
    return cover
  }

  private suspend fun Builder.setLargeIcon(book: Book): Builder {
    setLargeIcon(cover(book))
    return this
  }

  private fun Builder.setContentTitle(book: Book): Builder {
    setContentTitle(book.name)
    return this
  }

  private fun contentIntent(book: Book): PendingIntent {
    val contentIntent = toBookIntentProvider.goToBookIntent(book.id)
    return PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun Builder.setChapterInfo(book: Book): Builder {
    val chapters = book.content.chapters
    if (chapters.size > 1) {
      // we need the current chapter title and number only if there is more than one chapter.
      setContentInfo("${(book.content.currentChapterIndex + 1)}/${chapters.size}")
      setContentText(book.content.currentChapter.name)
    } else {
      setContentInfo(null)
      setContentText(null)
    }
    return this
  }

  private fun stopIntent(): PendingIntent {
    return buildMediaButtonPendingIntent(context, ACTION_STOP)
  }

  private fun Builder.addPlayPauseAction(playState: PlayStateManager.PlayState): Builder {
    return if (playState == PlayStateManager.PlayState.Playing) {
      addAction(pauseAction)
    } else {
      addAction(playAction)
    }
  }
}
