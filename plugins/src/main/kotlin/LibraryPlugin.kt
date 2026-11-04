import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure

class LibraryPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.pluginManager.run {
      apply("voice.ktfmt")
      apply("com.android.library")
      withPlugin("com.android.library") {
        target.baseSetup()
        checkModuleDependencies(target)
        target.extensions.configure<LibraryExtension> {
          androidResources.enable =
            target.layout.projectDirectory.dir("src/main/res").asFile.isDirectory
        }
        target.tasks.withType(Test::class.java).configureEach {
          // We want all modules to be configured for tests, for the
          // voiceUnitTest to work
          failOnNoDiscoveredTests.set(false)
        }
        target.tasks.register("voiceUnitTest") {
          dependsOn("testDebugUnitTest")
        }
      }
    }
  }
}
