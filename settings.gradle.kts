@file:Suppress("UnstableApiUsage")

rootProject.name = "voice"

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
      }
    }

    mavenCentral().mavenContent { releasesOnly() }
  }
}

plugins {
  id("com.gradle.enterprise") version "3.11.1"
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
include(":playbackScreen")
include(":sleepTimer")
include(":settings")
include(":search")
include(":folderPicker")
include(":bookOverview")
include(":migration")
include(":logging:core")
include(":logging:debug")
include(":logging:crashlytics")
