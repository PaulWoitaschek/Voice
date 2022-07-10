@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.baseSetup() {
  pluginManager.apply("org.jmailen.kotlinter")
  val libs: VersionCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
  tasks.withType(KotlinCompile::class.java).configureEach { kotlinCompile ->
    kotlinCompile.kotlinOptions {
      allWarningsAsErrors = true
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
  extensions.configure(KotlinProjectExtension::class.java) { kotlin ->
    kotlin.jvmToolchain {
      (it as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
    }
  }
  extensions.configure(BaseExtension::class.java) { baseExtension ->
    baseExtension.run {
      namespace = "voice." + path.removePrefix(":").replace(':', '.')
      compileOptions { compileOptions ->
        compileOptions.isCoreLibraryDesugaringEnabled = true
        compileOptions.sourceCompatibility = JavaVersion.VERSION_11
        compileOptions.targetCompatibility = JavaVersion.VERSION_11
      }
      defaultConfig { defaultConfig ->
        defaultConfig.multiDexEnabled = true
        defaultConfig.minSdk = libs.findVersion("sdk-min").get().requiredVersion.toInt()
        defaultConfig.targetSdk = libs.findVersion("sdk-target").get().requiredVersion.toInt()
      }
      compileSdkVersion(libs.findVersion("sdk-compile").get().requiredVersion.toInt())
      testOptions { testOptions ->
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.animationsDisabled = true
        testOptions.unitTests.isIncludeAndroidResources = true
      }
    }
  }
  dependencies.run {
    add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
    if (project.path != ":logging:core") {
      add("implementation", project(":logging:core"))
    }

    listOf(
      "coroutines.core",
      "coroutines.android",
    ).forEach {
      add("implementation", libs.findLibrary(it).get())
    }

    listOf(
      "junit",
      "koTest.assert",
      "mockk",
      "turbine",
      "coroutines.test"
    ).forEach {
      add("testImplementation", libs.findLibrary(it).get())
    }
  }
}
