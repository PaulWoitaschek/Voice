import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
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

fun Project.baseSetup() {
  val libs: VersionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
      freeCompilerArgs.addAll(
        "-Xannotation-default-target=param-property",
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
      languageVersion.set(JavaLanguageVersion.of(25))
    }
  }
  configureRobolectricSdk(this)
  extensions.configure<CommonExtension> {
    namespace = "voice." + path.removePrefix(":").replace(':', '.')
    compileOptions.apply {
      isCoreLibraryDesugaringEnabled = true
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
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
  }
  dependencies.run {
    add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
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
