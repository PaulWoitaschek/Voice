import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import java.io.File

// there is no global way to set the Robolectric SDK version,
// so we generate a file in the test resources
fun configureRobolectricSdk(target: Project) {
  val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  val generatedResourceDir = target.file("build/generated/testResources")
    .apply { mkdirs() }
  File(generatedResourceDir, "robolectric.properties").apply {
    val sdkVersion = libs.findVersion("sdk-robolectric").get().requiredVersion.toInt()
    writeText("sdk=$sdkVersion")
  }
  target.extensions.configure<CommonExtension> {
    sourceSets {
      named("test") {
        resources.directories.add(generatedResourceDir.path)
      }
    }
  }
}
