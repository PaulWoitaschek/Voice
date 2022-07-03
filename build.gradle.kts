@file:Suppress("UnstableApiUsage")

plugins {
  alias(libs.plugins.ktlint)
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.app) apply false
  alias(libs.plugins.android.library) apply false
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

tasks {
  register<Exec>("importStrings") {
    executable = "tx"
    args("pull")
    finalizedBy(":app:lintDebug")
  }

  register<TestReport>("allUnitTests") {
    val tests = subprojects.mapNotNull { subProject ->
      val tasks = subProject.tasks
      (
        tasks.findByName("testDebugUnitTest")
          ?: tasks.findByName("test")
        ) as? Test
    }
    val artifactFolder = File("${rootDir.absolutePath}/artifacts")
    destinationDir = File(artifactFolder, "testResults")
    reportOn(tests)
  }
}
