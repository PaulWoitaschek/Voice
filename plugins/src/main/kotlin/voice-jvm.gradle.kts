@file:Suppress("UnstableApiUsage")

plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(VoiceVersions.javaLanguageVersion)
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = VoiceVersions.javaCompileVersion.toString()
  }
}
