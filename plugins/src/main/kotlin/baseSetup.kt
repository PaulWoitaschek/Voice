@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.baseSetup() {
  pluginManager.apply("org.jmailen.kotlinter")
  val libs: VersionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      languageVersion = "1.9"
      jvmTarget = JavaVersion.VERSION_11.toString()
      val optIns = listOf(
        "kotlin.RequiresOptIn",
        "kotlin.ExperimentalStdlibApi",
        "kotlin.contracts.ExperimentalContracts",
        "kotlin.time.ExperimentalTime",
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
      )
      freeCompilerArgs = (freeCompilerArgs + listOf("-progressive") + optIns.map { "-opt-in=$it" })
    }
  }
  extensions.configure<KotlinProjectExtension> {
    jvmToolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
  }
  extensions.configure<BaseExtension> {
    namespace = "voice." + path.removePrefix(":").replace(':', '.')
    compileOptions {
      isCoreLibraryDesugaringEnabled = true
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
      multiDexEnabled = true
      minSdk = libs.findVersion("sdk-min").get().requiredVersion.toInt()
      targetSdk = libs.findVersion("sdk-target").get().requiredVersion.toInt()
    }
    compileSdkVersion(libs.findVersion("sdk-compile").get().requiredVersion.toInt())
    testOptions {
      unitTests.isReturnDefaultValues = true
      animationsDisabled = true
      unitTests.isIncludeAndroidResources = true
    }
  }
  dependencies.run {
    add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
    if (project.path != ":logging:core") {
      add("implementation", project(":logging:core"))
    }
    add("implementation", platform(libs.findLibrary("compose-bom").get()))
    if (providers.environmentVariable("VOICE_USE_PROPRIETARY_LIBRARIES").orNull == "true") {
      add("implementation", platform(libs.findLibrary("firebase-bom").get()))
    }
    add("androidTestImplementation", platform(libs.findLibrary("compose-bom").get()))

    listOf(
      "coroutines.core",
      "coroutines.android",
    ).forEach {
      add("implementation", libs.findLibrary(it).get())
    }

    add("testImplementation", libs.findBundle("testing-jvm").get())
  }
}
