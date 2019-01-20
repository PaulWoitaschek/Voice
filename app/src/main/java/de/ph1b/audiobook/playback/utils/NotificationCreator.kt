package de.ph1b.audiobook.playback.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.injection.PerService
import de.ph1b.audiobook.misc.PendingIntentCompat
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.MAX_IMAGE_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
  private val playerController: PlayerController,
  private val playStateManager: PlayStateManager,
  private val mediaSession: MediaSessionCompat,
  private val notificationChannelCreator: NotificationChannelCreator
) {

  private var cachedImage: CachedImage? = null

  private val mediaStyle = MediaStyle()
    .setShowActionsInCompactView(0, 1, 2)
    .setCancelButtonIntent(stopIntent())
    .setShowCancelButton(true)

  private val notificationBuilder =
    NotificationCompat.Builder(context, notificationChannelCreator.musicChannel)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
      .setDeleteIntent(stopIntent())
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setShowWhen(false)
      .setSmallIcon(R.drawable.ic_notification)
      .setStyle(mediaStyle)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setWhen(0)
  // notificationBuilder is not thread-save, we need to synchronize the access.
  private val notificationBuilderLock = Mutex()

  fun createDummyNotification(): Notification {
    val builder = NotificationCompat.Builder(context, notificationChannelCreator.musicChannel)
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setShowWhen(false)
      .setSmallIcon(R.drawable.ic_notification)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setWhen(0)
    return builder.build()
  }

  suspend fun createNotification(book: Book): Notification {
    return notificationBuilderLock.withLock {
      withContext(Dispatchers.IO) {
        mediaStyle.setMediaSession(mediaSession.sessionToken)
        @Suppress("RestrictedApi")
        notificationBuilder.mActions.clear()
        val playState = playStateManager.playState
        notificationBuilder
          .addRewindAction()
          .addPlayPauseAction(playState)
          .addFastForwardAction()
          .setChapterInfo(book)
          .setContentIntent(contentIntent(book))
          .setContentTitle(book)
          .setLargeIcon(book)
          .setOngoing(playState == PlayStateManager.PlayState.PLAYING)
          .build()
      }

    }
  }

  private suspend fun cover(book: Book): Bitmap {
    // first try to get use a cached image
    cachedImage?.let {
      if (it.matches(book)) return it.cover
    }

    val width = imageHelper.smallerScreenSize
    val height = imageHelper.smallerScreenSize

    // get the cover or fallback to a replacement
    val coverFile = book.coverFile()
    val picassoCover = if (coverFile.canRead() && coverFile.length() < MAX_IMAGE_SIZE) {
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

    val cover = picassoCover ?: imageHelper.drawableToBitmap(
      CoverReplacement(book.name, context),
      width,
      height
    )

    // add a cache entry
    cachedImage = CachedImage(book.id, cover)
    return cover
  }

  private suspend fun NotificationCompat.Builder.setLargeIcon(
    book: Book
  ): NotificationCompat.Builder {
    setLargeIcon(cover(book))
    return this
  }

  private fun NotificationCompat.Builder.setContentTitle(book: Book): NotificationCompat.Builder {
    setContentTitle(book.name)
    return this
  }

  private fun contentIntent(book: Book): PendingIntent {
    val contentIntent = MainActivity.goToBookIntent(context, book.id)
    return PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun NotificationCompat.Builder.setChapterInfo(book: Book): NotificationCompat.Builder {
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

  private fun stopIntent(): PendingIntent = PendingIntent.getService(
    context,
    KeyEvent.KEYCODE_MEDIA_STOP,
    playerController.stopIntent,
    PendingIntent.FLAG_UPDATE_CURRENT
  )

  private fun NotificationCompat.Builder.addFastForwardAction(): NotificationCompat.Builder {
    val fastForwardPI = PendingIntentCompat.getForegroundService(
      context,
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
      playerController.fastForwardAutoPlayIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )
    return addAction(
      R.drawable.ic_fast_forward_white_36dp,
      context.getString(R.string.fast_forward),
      fastForwardPI
    )
  }

  private fun NotificationCompat.Builder.addRewindAction(): NotificationCompat.Builder {
    val rewindPI = PendingIntentCompat.getForegroundService(
      context,
      KeyEvent.KEYCODE_MEDIA_REWIND,
      playerController.rewindAutoPlayerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )
    return addAction(R.drawable.ic_rewind_white_36dp, context.getString(R.string.rewind), rewindPI)
  }

  private fun NotificationCompat.Builder.addPlayPauseAction(
    playState: PlayStateManager.PlayState
  ): NotificationCompat.Builder {
    val playPausePI = PendingIntentCompat.getForegroundService(
      context,
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
      playerController.playPauseIntent,
      PendingIntent.FLAG_UPDATE_CURRENT
    )
    return if (playState == PlayStateManager.PlayState.PLAYING) {
      addAction(R.drawable.ic_pause_white_36dp, context.getString(R.string.pause), playPausePI)
    } else {
      addAction(R.drawable.ic_play_white_36dp, context.getString(R.string.play), playPausePI)
    }
  }
}
