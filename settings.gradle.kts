@file:Suppress("UnstableApiUsage")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    includeBuild("plugins")
  }
}

dependencyResolutionManagement {
  repositories {
    google()

    exclusiveContent {
      forRepository {
        maven(url = "https://jitpack.io")
      }
      filter {
        includeGroupByRegex("com.github.PaulWoitaschek.*")
        includeModule("com.github.lisawray.groupie", "groupie")
      }
    }

    mavenCentral().mavenContent { releasesOnly() }
  }
}

plugins {
  id("com.gradle.enterprise") version "3.8.1"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(":app")
include(":strings")
include(":common")
include(":data")
include(":playback")
include(":ffmpeg")
include(":scanner")
include(":core")
include(":playbackScreen")
include(":sleepTimer")
include(":settings")
include(":folderPicker")
include(":benchmark")
include(":bookOverview")
include(":migration")
include(":logging:core")
include(":logging:debug")
include(":logging:crashlytics")
