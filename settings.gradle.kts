@file:Suppress("UnstableApiUsage")

import org.gradle.api.internal.FeaturePreviews

rootProject.name = "voice"

FeaturePreviews.Feature.entries.forEach { feature ->
  val enable = when (feature) {
    FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS,
    FeaturePreviews.Feature.STABLE_CONFIGURATION_CACHE,
    FeaturePreviews.Feature.NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS,
      -> true
    FeaturePreviews.Feature.GROOVY_COMPILATION_AVOIDANCE,
    FeaturePreviews.Feature.INTERNAL_BUILD_SERVICE_USAGE,
    FeaturePreviews.Feature.ALWAYS_INACTIVE,
      -> false
  }
  if (enable) {
    check(feature.isActive){
      "Feature $feature is not active"
    }
    enableFeaturePreview(feature.name)
  }
}

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
    mavenCentral()
  }
}

plugins {
  id("com.gradle.develocity") version "4.5.0"
  id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
  id("org.jetbrains.kotlin.android") version "2.4.0" apply false
  id("org.jetbrains.kotlin.jvm") version "2.4.0" apply false
  id("com.android.application") version "9.2.1" apply false
  id("com.android.library") version "9.2.1" apply false
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
include(":features:support:api")
include(":features:support:free")
include(":features:support:play")
include(":features:widget")
include(":core:featureflag")
