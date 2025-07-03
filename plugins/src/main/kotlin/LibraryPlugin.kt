import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.run {
      apply("voice.ktlint")
      apply("com.android.library")
      apply("kotlin-android")
      withPlugin("com.android.library") {
        target.baseSetup()
        target.tasks.register("voiceUnitTest") {
          dependsOn("testDebugUnitTest")
        }
      }
    }
  }
}
