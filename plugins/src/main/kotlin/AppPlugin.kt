import org.gradle.api.Plugin
import org.gradle.api.Project

class AppPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.run {
      apply("com.android.application")
      apply("kotlin-android")
      withPlugin("com.android.application") {
        target.baseSetup()
      }
    }
  }
}
