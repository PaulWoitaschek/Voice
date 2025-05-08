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
  id("com.gradle.develocity") version "4.0.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.10.0")
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
  }
}

include(":app")
include(":strings")
include(":common")
include(":bookmark")
include(":data")
include(":playback")
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
include(":scripts")
include(":pref")
include(":logging:core")
include(":logging:debug")
include(":documentfile")
include(":onboarding")
include(":logging:crashlytics")
include(":review:play")
include(":review:noop")
include(":remoteconfig:core")
include(":remoteconfig:firebase")
include(":remoteconfig:noop")
