import com.android.build.api.dsl.AndroidTest
import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.TestFixtures
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.UnitTest
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradleExtension

fun Project.baseSetup() {
  val project = this
  val libs: VersionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  configurePowerAssert(libs)

  val jvmBytecodeVersion = libs.findVersion("jvm-bytecode").get().requiredVersion.toInt()
  val jvmToolchainVersion = libs.findVersion("jvm-toolchain").get().requiredVersion.toInt()
  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.fromTarget(jvmBytecodeVersion.toString()))
      freeCompilerArgs.addAll(
        "-Xreturn-value-checker=full",
      )
      optIn.addAll(
        listOf(
          "kotlin.RequiresOptIn",
          "kotlin.ExperimentalStdlibApi",
          "kotlin.contracts.ExperimentalContracts",
          "kotlin.time.ExperimentalTime",
          "kotlinx.coroutines.ExperimentalCoroutinesApi",
          "kotlinx.coroutines.FlowPreview",
        ),
      )
      allWarningsAsErrors.set(providers.gradleProperty("voice.warningsAsErrors").map(String::toBooleanStrict))
    }
  }
  tasks.withType(Test::class.java).configureEach {
    // run tests in parallel https://docs.gradle.org/current/userguide/performance.html#a_run_tests_in_parallel
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  }
  extensions.configure<KotlinProjectExtension> {
    jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(jvmToolchainVersion))
    }
  }
  configureRobolectricSdk(this)
  extensions.configure<CommonExtension> {
    namespace = "voice." + path.removePrefix(":").replace(':', '.')
    compileOptions.apply {
      sourceCompatibility = JavaVersion.toVersion(jvmBytecodeVersion)
      targetCompatibility = JavaVersion.toVersion(jvmBytecodeVersion)
    }
    defaultConfig.apply {
      if (this is ApplicationDefaultConfig) {
        multiDexEnabled = true
        targetSdk = libs.findVersion("sdk-target").get().requiredVersion.toInt()
      }
      minSdk = libs.findVersion("sdk-min").get().requiredVersion.toInt()
    }
    compileSdk = libs.findVersion("sdk-compile").get().requiredVersion.toInt()
    testOptions.apply {
      unitTests.isReturnDefaultValues = true
      animationsDisabled = true
      unitTests.isIncludeAndroidResources = true
    }
    lint.lintConfig = project.layout.settingsDirectory.file("lint.xml").asFile
  }
  dependencies.run {
    add("implementation", platform(libs.findLibrary("compose-bom").get()))
    add("implementation", platform(libs.findLibrary("firebase-bom").get()))
    add("implementation", libs.findLibrary("coroutines.core").get())
    add("implementation", libs.findLibrary("coroutines.android").get())
    if (project.path != ":core:logging:api") {
      add("implementation", project(":core:logging:api"))
    }
    add("testImplementation", libs.findBundle("testing-jvm").get())
  }
}

@Suppress("OPT_IN_USAGE")
private fun Project.configurePowerAssert(libs: VersionCatalog) {
  pluginManager.apply("org.jetbrains.kotlin.plugin.power-assert")
  extensions.configure<PowerAssertGradleExtension> {
    addRuntimeDependency.set(false)
    functions.addAll(
      "kotlin.assert",
      "kotlin.test.assertTrue",
      "kotlin.test.assertEquals",
      "kotlin.test.assertNull",
    )
  }
  extensions.getByType(AndroidComponentsExtension::class.java)
    .onVariants(extensions.getByType(AndroidComponentsExtension::class.java).selector().all()) { variant ->
      val testSourceSets = variant.nestedComponents.filter {
        it is UnitTest || it is AndroidTest || it is TestFixtures
      }.map {
        it.name
      }
      extensions.configure<PowerAssertGradleExtension> {
        includedSourceSets.addAll(
          testSourceSets,
        )
      }
    }

  dependencies.run {
    add("testImplementation", libs.findLibrary("kotlin.powerAssert.runtime").get())
    add("androidTestImplementation", libs.findLibrary("kotlin.powerAssert.runtime").get())
  }
}
