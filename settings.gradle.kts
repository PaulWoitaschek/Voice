enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  @Suppress("UnstableApiUsage")
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.squareup.anvil") version "2.3.3"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.6.3"
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
include(":covercolorextractor")
include(":playback")
include(":prefs")
include(":ffmpeg")
include(":scanner")
include(":core")
include(":playbackScreen")
include(":sleepTimer")
include(":loudness")
include(":settings")
