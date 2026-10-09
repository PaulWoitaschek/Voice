package voice.core.audiobookshelf

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
@Inject
class AudiobookshelfInitializer(
  private val sync: AudiobookshelfSync,
  private val downloads: AudiobookshelfDownloads,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    sync.start()
    downloads.start()
    // coming back to Voice after listening on another device should pick up there
    application.registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        private var startedActivities = 0

        override fun onActivityStarted(activity: Activity) {
          if (startedActivities++ == 0) {
            sync.sync()
            downloads.onAppVisible()
          }
        }

        override fun onActivityStopped(activity: Activity) {
          if (--startedActivities == 0) downloads.onAppHidden()
        }

        override fun onActivityCreated(
          activity: Activity,
          savedInstanceState: Bundle?,
        ) {}

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivitySaveInstanceState(
          activity: Activity,
          outState: Bundle,
        ) {}

        override fun onActivityDestroyed(activity: Activity) {}
      },
    )
  }
}
