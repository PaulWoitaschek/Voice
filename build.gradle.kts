@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    classpath(libs.androidPluginForGradle)
    classpath(libs.kotlin.pluginForGradle)
  }
}

plugins {
  alias(libs.plugins.ktlint)
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

allprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf(
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
    finalizedBy(":app:lintDebug")
  }

  register<TestReport>("allUnitTests") {
    val tests = subprojects.mapNotNull { subProject ->
      val tasks = subProject.tasks
      (
        tasks.findByName("testReleaseUnitTest")
          ?: tasks.findByName("testDebugUnitTest")
          ?: tasks.findByName("test")
        ) as? Test
    }
    val artifactFolder = File("${rootDir.absolutePath}/artifacts")
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }
}
