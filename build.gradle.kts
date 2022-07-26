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
  val pullStrings = register<Exec>("pullStrings") {
    executable = "tx"
    args("pull", "-a", "--parallel")
  }
  val removeEmptyListings = register("removeEmptyListings") {
    dependsOn(pullStrings)
    mustRunAfter(pullStrings)
    val listingsFolder = file("app/src/main/play/listings")
    doLast {
      (listingsFolder.listFiles()?.toList() ?: emptyList())
        .filter { it.isDirectory }
        .filterNot {
          // don't remove the source language
          it.name == "en-US"
        }
        .forEach { language ->
          val contents = language.listFiles()?.toList() ?: emptyList()
          val requiredListings = listOf("full-description.txt", "short-description.txt", "title.txt")
            .map { File(language, it) }
          if (!contents.containsAll(requiredListings)) {
            logger.warn("Missing translations for ${language.name}. Delete listing.")
            language.deleteRecursively()
          }
        }
    }
  }
  register("importStrings") {
    dependsOn(pullStrings, removeEmptyListings)
    mustRunAfter(pullStrings, removeEmptyListings)
    finalizedBy(":app:lintDebug")
  }
}
