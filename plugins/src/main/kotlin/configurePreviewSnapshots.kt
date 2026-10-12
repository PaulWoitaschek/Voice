import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault

private const val SNAPSHOT_TEST = "voice.core.snapshot.PreviewSnapshots"
private const val SNAPSHOT_TEST_CLASS = "voice/core/snapshot/PreviewSnapshots.class"

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

  // excluded from scanning rather than filtered, so a `--tests` pattern that matches nothing still fails
  tasks.withType<Test>().configureEach {
    if (name != "snapshot") {
      exclude(SNAPSHOT_TEST_CLASS)
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
    // the Android resources reach Robolectric through a properties file with paths, so they need declaring
    inputs.files(unitTest.inputs.files)
      .withPropertyName("unitTestInputs")
      .withPathSensitivity(PathSensitivity.RELATIVE)
    useJUnit()
    include(SNAPSHOT_TEST_CLASS)
    systemProperty("java.awt.headless", "true")
    jvmArgumentProviders += SnapshotDir(snapshotDir)
    // previews that were removed shouldn't leave their images behind
    doFirst { snapshotDir.get().asFile.deleteRecursively() }
  }
}

@DisableCachingByDefault(because = "Writing one small file is faster than the cache")
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
