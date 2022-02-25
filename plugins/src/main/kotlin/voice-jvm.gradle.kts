@file:Suppress("UnstableApiUsage")

plugins {
  kotlin("jvm")
  id("com.android.lint")
}

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(VoiceVersions.javaLanguageVersion)
  }
}

baseSetup()
