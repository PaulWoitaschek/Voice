import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

// there is no global way to set the Robolectric SDK version,
// so we generate a file in the test resources
fun configureRobolectricSdk(target: Project) {
  val libs = target.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  val sdkVersion = libs.findVersion("sdk-robolectric").get().requiredVersion.toInt()
  val androidComponents = target.extensions.getByType(AndroidComponentsExtension::class.java)
  androidComponents.onVariants(androidComponents.selector().all()) { variant ->
    val unitTest = (variant as? HasHostTests)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE) ?: return@onVariants
    val generate = target.tasks.register<GenerateRobolectricProperties>(
      "generate${variant.name.replaceFirstChar(Char::uppercase)}RobolectricProperties",
    ) {
      sdk.set(sdkVersion)
    }
    unitTest.sources.resources?.addGeneratedSourceDirectory(generate, GenerateRobolectricProperties::outputDir)
  }
}

@DisableCachingByDefault(because = "Writing one small file is faster than the cache")
abstract class GenerateRobolectricProperties : DefaultTask() {

  @get:Input
  abstract val sdk: Property<Int>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    outputDir.get().file("robolectric.properties").asFile.writeText("sdk=${sdk.get()}")
  }
}
