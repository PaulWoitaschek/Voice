package voice.core.audiobookshelf.download

import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.strings.R as StringsR

class AudiobookshelfDownloadService :
  DownloadService(
    DOWNLOAD_NOTIFICATION_ID,
    1000,
    DOWNLOAD_CHANNEL_ID,
    StringsR.string.audiobookshelf_downloads_channel,
    0,
  ) {

  private val downloads by lazy { rootGraphAs<AudiobookshelfDownloadGraph>().audiobookshelfDownloads }
  private val scope = MainScope()

  override fun onCreate() {
    super.onCreate()
    scope.launch {
      downloads.foregroundNotificationChanges.collect {
        // once the downloads are done the service stops, an update then would leave the notification behind
        if (downloads.hasActiveDownloads()) invalidateForegroundNotification()
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
    NotificationManagerCompat.from(this).cancel(DOWNLOAD_NOTIFICATION_ID)
  }

  override fun getDownloadManager(): DownloadManager = downloads.downloadManager

  // since Android 12 a scheduler can't start the service from the background anyway
  override fun getScheduler(): Scheduler? = null

  override fun getForegroundNotification(
    downloads: List<Download>,
    notMetRequirements: Int,
  ): Notification = this.downloads.foregroundNotification(downloads)
}

@ContributesTo(AppScope::class)
interface AudiobookshelfDownloadGraph {
  val audiobookshelfDownloads: AudiobookshelfDownloads
}
