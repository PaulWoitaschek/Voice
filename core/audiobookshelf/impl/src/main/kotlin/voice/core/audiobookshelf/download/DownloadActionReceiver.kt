package voice.core.audiobookshelf.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import voice.core.common.rootGraphAs
import voice.core.data.BookId

/**
 * The buttons of the download notifications.
 */
class DownloadActionReceiver : BroadcastReceiver() {

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    val downloads = rootGraphAs<AudiobookshelfDownloadGraph>().audiobookshelfDownloads
    val pendingResult = goAsync()
    downloads.onNotificationAction(intent, onDone = pendingResult::finish)
  }

  internal companion object {

    const val ACTION_STOP = "voice.audiobookshelf.download.STOP"
    const val ACTION_RETRY = "voice.audiobookshelf.download.RETRY"

    fun stop(
      context: Context,
      bookIds: List<BookId>,
    ): PendingIntent = pendingIntent(context, ACTION_STOP, bookIds)

    fun retry(
      context: Context,
      bookId: BookId,
    ): PendingIntent = pendingIntent(context, ACTION_RETRY, listOf(bookId))

    private fun pendingIntent(
      context: Context,
      action: String,
      bookIds: List<BookId>,
    ): PendingIntent {
      val intent = Intent(context, DownloadActionReceiver::class.java)
        .setAction(action)
        // one intent per book, so the buttons of two books don't replace each other
        .setData(Uri.fromParts("voice", bookIds.joinToString(",") { it.value }, null))
        .withBookIds(bookIds)
      return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
