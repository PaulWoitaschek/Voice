@file:Suppress("UnstableApiUsage")

@Suppress("RemoveRedundantQualifierName")
buildscript {
  dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>()
      .named("libs") as org.gradle.accessors.dm.LibrariesForLibs
    classpath(libs.androidGradlePlugin)
    classpath(libs.kotlin.gradlePlugin)
    classpath(libs.serialization.gradlePlugin)
  }
}

plugins {
  id("com.github.ben-manes.versions") version "0.38.0"
  id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

allprojects {
  plugins.withType(com.android.build.gradle.internal.plugins.BasePlugin::class.java) {
    with(extension) {
      defaultConfig {
        multiDexEnabled = true
        minSdk = 24
        targetSdk = 30
      }
      compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
      }
      compileSdkVersion(31)

      composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
      }

      dependencies {
        add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:1.1.1")
      }
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
      freeCompilerArgs = listOf(
        "-Xinline-classes",
        "-progressive",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlinx.coroutines.FlowPreview",
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi",
        "-Xopt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
      )
    }
  }
}

subprojects {
  fun addCoreDependencies() {
    if (path != ":core") {
      dependencies.add("implementation", projects.core)
    }
  }
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  plugins.withId("kotlin") {
    addCoreDependencies()
  }
  plugins.withId("kotlin-android") {
    addCoreDependencies()
  }
}

tasks {
  register<Exec>("importStrings") {
    executable = "sh"
    args("-c", "tx pull -af --minimum-perc=5")
    finalizedBy(":core:lintDebug")
  }

  register<TestReport>("allUnitTests") {
    val tests = subprojects.mapNotNull { subProject ->
      val tasks = subProject.tasks
      (
        tasks.findByName("testReleaseUnitTest")
          ?: tasks.findByName("test")
        ) as? Test
    }
    val artifactFolder = File("${rootDir.absolutePath}/artifacts")
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }
}

apply(from = "dependency_updates.gradle")
