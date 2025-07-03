import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import java.io.File

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
        configureRobolectricSdk(target)
      }
    }
  }

  // there is no global way to set the Robolectric SDK version,
  // so we generate a file in the test resources
  private fun configureRobolectricSdk(target: Project) {
    val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val generatedResourceDir = target.file("build/generated/testResources")
      .apply { mkdirs() }
    File(generatedResourceDir, "robolectric.properties").apply {
      val sdkVersion = libs.findVersion("sdk-robolectric").get().requiredVersion.toInt()
      writeText("sdk=$sdkVersion")
    }
    target.extensions.configure<LibraryExtension> {
      sourceSets {
        named("test") {
          resources.srcDir(generatedResourceDir)
        }
      }
    }
  }
}
