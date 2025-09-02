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
  id("com.gradle.develocity") version "4.1.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
  id("org.jetbrains.kotlin.android") version "2.2.10" apply false
  id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
  id("com.android.application") version "8.13.0" apply false
  id("com.android.library") version "8.13.0" apply false
  id("com.autonomousapps.build-health") version "3.0.1"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
  }
}

include(":app")
include(":navigation")
include(":scripts")

include(":core:common")
include(":core:data:api")
include(":core:data:impl")
include(":core:documentfile")
include(":core:logging:core")
include(":core:logging:crashlytics")
include(":core:logging:debug")
include(":core:playback")
include(":core:remoteconfig:core")
include(":core:remoteconfig:firebase")
include(":core:remoteconfig:noop")
include(":core:scanner")
include(":core:search")
include(":core:strings")
include(":core:ui")
include(":core:initializer")

include(":features:bookOverview")
include(":features:bookmark")
include(":features:cover")
include(":features:folderPicker")
include(":features:onboarding")
include(":features:playbackScreen")
include(":features:review:noop")
include(":features:review:play")
include(":features:settings")
include(":features:sleepTimer")
include(":features:widget")
include(":core:sleeptimer:api")
include(":core:sleeptimer:impl")
