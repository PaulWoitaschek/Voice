package de.ph1b.audiobook.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.app.NotificationCompat
import android.view.KeyEvent
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.BookActivity
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import e
import java.io.IOException
import javax.inject.Inject

/**
 * Provides Notifications based on playing information.
 *
 * @author Paul Woitaschek
 */
class NotificationAnnouncer
@Inject constructor(private val context: Context, private val imageHelper: ImageHelper, private val serviceController: ServiceController) {

    fun getNotification(book: Book, playState: PlayStateManager.PlayState, sessionToken: MediaSessionCompat.Token): Notification {
        // cover
        val width = imageHelper.smallerScreenSize
        val height = imageHelper.smallerScreenSize
        var cover: Bitmap? = null
        try {
            val coverFile = book.coverFile()
            if (!book.useCoverReplacement && coverFile.exists() && coverFile.canRead()) {
                cover = Picasso.with(context)
                        .load(coverFile)
                        .resize(width, height)
                        .get()
            }
        } catch (ex: IOException) {
            e(ex) { "Error when retrieving cover from $book" }
        }

        if (cover == null) {
            cover = imageHelper.drawableToBitmap(CoverReplacement(book.name, context), width, height)
        }

        val notificationBuilder = NotificationCompat.Builder(context)
        val chapter = book.currentChapter()

        val chapters = book.chapters
        if (chapters.size > 1) {
            // we need the current chapter title and number only if there is more than one chapter.
            notificationBuilder.setContentInfo("${(chapters.indexOf(chapter) + 1)}/${chapters.size}")
            notificationBuilder.setContentText(chapter.name)
        }

        // rewind
        val rewindIntent = serviceController.getRewindIntent()
        val rewindPI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_REWIND, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_rewind_white_36dp, context.getString(R.string.rewind), rewindPI)

        // play/pause
        val playPauseIntent = serviceController.getPlayPauseIntent()
        val playPausePI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (playState == PlayStateManager.PlayState.PLAYING) {
            notificationBuilder.addAction(R.drawable.ic_pause_white_36dp, context.getString(R.string.pause), playPausePI)
        } else {
            notificationBuilder.addAction(R.drawable.ic_play_white_36dp, context.getString(R.string.play), playPausePI)
        }

        // fast forward
        val fastForwardIntent = serviceController.getFastForwardIntent()
        val fastForwardPI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_fast_forward_white_36dp, context.getString(R.string.fast_forward), fastForwardPI)

        // stop intent
        val stopIntent = serviceController.getStopIntent()
        val stopPI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // content click
        val contentIntent = BookActivity.goToBookIntent(context, book.id)
        val contentPI = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return notificationBuilder
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setCancelButtonIntent(stopPI)
                        .setShowCancelButton(true)
                        .setMediaSession(sessionToken))
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPI)
                .setContentTitle(book.name)
                .setSmallIcon(R.drawable.ic_notification)
                .setWhen(0)
                .setDeleteIntent(stopPI)
                .setAutoCancel(true)
                .setLargeIcon(cover)
                .build()
    }
}