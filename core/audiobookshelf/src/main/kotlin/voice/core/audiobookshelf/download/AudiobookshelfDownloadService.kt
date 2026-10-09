package voice.core.audiobookshelf.download

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.core.common.rootGraphAs
import voice.core.playback.R as PlaybackR
import voice.core.strings.R as StringsR

private const val NOTIFICATION_ID = 4242
private const val CHANNEL_ID = "audiobookshelfDownloads"

class AudiobookshelfDownloadService :
  DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    StringsR.string.audiobookshelf_downloads_channel,
    0,
  ) {

  private val notificationHelper by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

  override fun getDownloadManager(): DownloadManager {
    return rootGraphAs<AudiobookshelfDownloadGraph>().audiobookshelfDownloads.downloadManager
  }

  // downloads go on when Voice starts the next time
  override fun getScheduler(): Scheduler? = null

  override fun getForegroundNotification(
    downloads: List<Download>,
    notMetRequirements: Int,
  ): Notification {
    return notificationHelper.buildProgressNotification(
      this,
      PlaybackR.drawable.ic_notification,
      null,
      null,
      downloads,
      notMetRequirements,
    )
  }
}

@ContributesTo(AppScope::class)
interface AudiobookshelfDownloadGraph {
  val audiobookshelfDownloads: AudiobookshelfDownloads
}
