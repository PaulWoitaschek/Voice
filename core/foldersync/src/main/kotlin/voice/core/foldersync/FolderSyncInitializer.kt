package voice.core.foldersync

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
@Inject
public class FolderSyncInitializer(private val folderSync: FolderSync) : AppInitializer {

  override fun onAppStart(application: Application) {
    folderSync.start()
    // coming back to Voice after listening on another device should pick up there
    application.registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        private var startedActivities = 0

        override fun onActivityStarted(activity: Activity) {
          if (startedActivities++ == 0) folderSync.requestSync()
        }

        override fun onActivityStopped(activity: Activity) {
          startedActivities--
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
