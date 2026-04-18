@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "plugins"

plugins {
  if(System.getenv("CI") != "true" || System.getenv("COPILOT_AGENT_FIREWALL_ENABLE_RULESET_ALLOW_LIST")!= null) {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
