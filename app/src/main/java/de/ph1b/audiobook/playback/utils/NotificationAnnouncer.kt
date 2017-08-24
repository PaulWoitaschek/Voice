package de.ph1b.audiobook.playback.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.app.NotificationCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.misc.PendingIntentCompat
import de.ph1b.audiobook.misc.getOnUiThread
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.maxImageSize
import javax.inject.Inject

/**
 * Provides Notifications based on playing information.
 */
class NotificationAnnouncer
@Inject constructor(
    private val context: Context,
    private val imageHelper: ImageHelper,
    private val serviceController: ServiceController,
    private val notificationChannelCreator: NotificationChannelCreator
) {

  private var cachedImage: CachedImage? = null

  fun getNotification(book: Book?, playState: PlayStateManager.PlayState, sessionToken: MediaSessionCompat.Token): Notification {
    val stopPI = stopIntent()
    val mediaStyle = MediaStyle()
        .setShowActionsInCompactView(0, 1, 2)
        .setCancelButtonIntent(stopPI)
        .setShowCancelButton(true)
        .setMediaSession(sessionToken)
    return NotificationCompat.Builder(context, notificationChannelCreator.musicChannel)
        .addRewindAction()
        .addPlayPauseAction(playState)
        .addFastForwardAction()
        .setStyle(mediaStyle)
        .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setChapterInfo(book)
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentTitle(book)
        .setContentIntent(contentIntent(book))
        .setSmallIcon(R.drawable.ic_notification)
        .setLargeIcon(book)
        .setWhen(0)
        .setDeleteIntent(stopPI)
        .setAutoCancel(true)
        .build()
  }

  private fun cover(book: Book): Bitmap {
    // first try to get use a cached image
    cachedImage?.let {
      if (it.matches(book)) return it.cover
    }

    val width = imageHelper.smallerScreenSize
    val height = imageHelper.smallerScreenSize

    // get the cover or fallback to a replacement
    val picassoCover = if (book.coverFile().canRead() && book.coverFile().length() < maxImageSize) {
      Picasso.with(context)
          .load(book.coverFile())
          .resize(width, height)
          .getOnUiThread()
    } else null

    val cover = picassoCover ?: imageHelper.drawableToBitmap(CoverReplacement(book.name, context), width, height)

    // add a cache entry
    cachedImage = CachedImage(book.id, cover)
    return cover
  }

  private fun NotificationCompat.Builder.setLargeIcon(book: Book?): NotificationCompat.Builder {
    book?.let {
      setLargeIcon(cover(book))
    }
    return this
  }

  private fun NotificationCompat.Builder.setContentTitle(book: Book?): NotificationCompat.Builder {
    book?.let { setContentTitle(it.name) }
    return this
  }

  private fun contentIntent(book: Book?): PendingIntent {
    val contentIntent = if (book != null) {
      MainActivity.goToBookIntent(context, book.id)
    } else MainActivity.newIntent(context, false)
    return PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun NotificationCompat.Builder.setChapterInfo(book: Book?): NotificationCompat.Builder {
    if (book == null) return this

    val chapters = book.chapters
    val currentChapter = book.currentChapter()
    if (chapters.size > 1) {
      // we need the current chapter title and number only if there is more than one chapter.
      setContentInfo("${(chapters.indexOf(currentChapter) + 1)}/${chapters.size}")
      setContentText(currentChapter.name)
    }
    return this
  }

  private fun stopIntent(): PendingIntent {
    val stopIntent = serviceController.getStopIntent()
    return PendingIntentCompat.getForegroundService(context, KeyEvent.KEYCODE_MEDIA_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  private fun NotificationCompat.Builder.addFastForwardAction(): NotificationCompat.Builder {
    val fastForwardIntent = serviceController.getFastForwardIntent()
    val fastForwardPI = PendingIntentCompat.getForegroundService(
        context,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        fastForwardIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    return addAction(R.drawable.ic_fast_forward_white_36dp, context.getString(R.string.fast_forward), fastForwardPI)
  }

  private fun NotificationCompat.Builder.addRewindAction(): NotificationCompat.Builder {
    val rewindIntent = serviceController.getRewindIntent()
    val rewindPI = PendingIntentCompat.getForegroundService(
        context,
        KeyEvent.KEYCODE_MEDIA_REWIND,
        rewindIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    return addAction(R.drawable.ic_rewind_white_36dp, context.getString(R.string.rewind), rewindPI)
  }

  private fun NotificationCompat.Builder.addPlayPauseAction(playState: PlayStateManager.PlayState): NotificationCompat.Builder {
    val playPauseIntent = serviceController.getPlayPauseIntent()
    val playPausePI = PendingIntentCompat.getForegroundService(
        context,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        playPauseIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    return if (playState == PlayStateManager.PlayState.PLAYING) {
      addAction(R.drawable.ic_pause_white_36dp, context.getString(R.string.pause), playPausePI)
    } else {
      addAction(R.drawable.ic_play_white_36dp, context.getString(R.string.play), playPausePI)
    }
  }
}
