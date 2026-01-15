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
  id("com.gradle.develocity") version "4.3.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
  id("org.jetbrains.kotlin.android") version "2.3.0" apply false
  id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
  id("com.android.application") version "8.13.2" apply false
  id("com.android.library") version "8.13.2" apply false
  id("com.autonomousapps.build-health") version "3.5.1"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
  }
}

include(":app")
include(":navigation")

include(":core:analytics:api")
include(":core:analytics:noop")
include(":core:analytics:firebase")
include(":core:common")
include(":core:data:api")
include(":core:data:impl")
include(":core:documentfile")
include(":core:initializer")
include(":core:logging:api")
include(":core:logging:crashlytics")
include(":core:logging:debug")
include(":core:playback")
include(":core:remoteconfig:api")
include(":core:remoteconfig:firebase")
include(":core:remoteconfig:noop")
include(":core:scanner")
include(":core:search")
include(":core:sleeptimer:api")
include(":core:sleeptimer:impl")
include(":core:strings")
include(":core:ui")

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
include(":core:featureflag")