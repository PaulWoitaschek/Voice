import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

private const val SNAPSHOT_TEST = "voice.core.snapshot.PreviewSnapshots"

/**
 * Registers `snapshot`, which renders every `@Preview` of the module to `build/snapshots`.
 * It runs on demand only: the regular unit test tasks skip it.
 */
internal fun Project.configurePreviewSnapshots() {
  val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  dependencies.add("testImplementation", project(":core:snapshot"))
  // the test activity the previews are rendered in
  dependencies.add("debugImplementation", libs.findLibrary("compose-ui-testManifest").get())

  val generate = tasks.register<GeneratePreviewSnapshotTest>("generatePreviewSnapshotTest") {
    outputDir.set(layout.buildDirectory.dir("generated/previewSnapshotTest"))
  }
  extensions.getByType<LibraryAndroidComponentsExtension>().apply {
    onVariants(selector().withBuildType("debug")) { variant ->
      variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]
        ?.sources
        ?.kotlin
        ?.addGeneratedSourceDirectory(generate, GeneratePreviewSnapshotTest::outputDir)
    }
  }

  tasks.withType<Test>().configureEach {
    if (name != "snapshot") {
      filter.excludeTestsMatching(SNAPSHOT_TEST)
      // modules without tests would fail on the exclude
      filter.isFailOnNoMatchingTests = false
    }
  }
  val snapshotDir = layout.buildDirectory.dir("snapshots")
  tasks.register<Test>("snapshot") {
    description = "Renders every @Preview of this module to build/snapshots."
    group = "verification"
    // eagerly, a mapped provider would make it depend on running the unit tests
    val unitTest = tasks.named("testDebugUnitTest", Test::class.java).get()
    testClassesDirs = unitTest.testClassesDirs
    classpath = unitTest.classpath
    javaLauncher.set(unitTest.javaLauncher)
    useJUnit()
    filter.includeTestsMatching(SNAPSHOT_TEST)
    systemProperty("java.awt.headless", "true")
    jvmArgumentProviders += SnapshotDir(snapshotDir)
    // previews that were removed shouldn't leave their images behind
    doFirst { snapshotDir.get().asFile.deleteRecursively() }
  }
}

@CacheableTask
abstract class GeneratePreviewSnapshotTest : DefaultTask() {

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val dir = outputDir.get().asFile
    dir.deleteRecursively()
    dir.resolve(SNAPSHOT_TEST.replace('.', '/') + ".kt").apply {
      parentFile.mkdirs()
      writeText(
        """
        |package voice.core.snapshot
        |
        |class PreviewSnapshots : PreviewSnapshotTest()
        |
        """.trimMargin(),
      )
    }
  }
}

private class SnapshotDir(
  @get:OutputDirectory val dir: Provider<Directory>,
) : CommandLineArgumentProvider {

  override fun asArguments(): Iterable<String> = listOf("-Dvoice.snapshot.dir=${dir.get().asFile.absolutePath}")
}
