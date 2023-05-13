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

    mavenCentral()
  }
}

plugins {
  id("com.gradle.enterprise") version "3.13.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.5.0")
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
include(":cover")
include(":datastore")
include(":folderPicker")
include(":bookOverview")
include(":migration")
include(":logging:core")
include(":logging:debug")
include(":logging:crashlytics")
include(":documentfile")