@file:Suppress("UnstableApiUsage")

plugins {
  id("com.android.application")
  id("kotlin-android")
}

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(VoiceVersions.javaLanguageVersion)
  }
}

baseSetup()

android {
  defaultConfig {
    multiDexEnabled = true
    minSdk = VoiceVersions.minSdk
    targetSdk = VoiceVersions.targetSdk
  }
  compileSdk = VoiceVersions.compileSdk
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = VoiceVersions.javaCompileVersion
    targetCompatibility = VoiceVersions.javaCompileVersion
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.composeVersion
  }
}

dependencies {
  coreLibraryDesugaring(libs.desugar)
}
