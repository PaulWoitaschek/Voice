@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.baseSetup() {
  val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_11.toString()
      freeCompilerArgs = freeCompilerArgs + listOf(
        "-progressive",
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlin.ExperimentalStdlibApi",
        "-opt-in=kotlin.time.ExperimentalTime",
        "-opt-in=kotlinx.coroutines.FlowPreview",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlin.contracts.ExperimentalContracts",
        "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
      )
    }
  }
  extensions.configure<KotlinProjectExtension> {
    jvmToolchain {
      (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
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
    composeOptions {
      kotlinCompilerExtensionVersion = libs.findVersion("compose").get().requiredVersion
    }
    testOptions {
      unitTests.isReturnDefaultValues = true
      animationsDisabled = true
      unitTests.isIncludeAndroidResources = true
    }
  }
  dependencies {
    add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
    if(project.path != ":logging:core"){
      add("implementation", project(":logging:core"))
    }
  }
}
