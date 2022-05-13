@file:Suppress("UnstableApiUsage")

plugins {
  id("com.android.library")
  id("kotlin-android")
}

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

baseSetup()

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(VoiceVersions.javaLanguageVersion)
  }
}

android {
  defaultConfig {
    multiDexEnabled = true
    minSdk = VoiceVersions.minSdk
    targetSdk = VoiceVersions.targetSdk
  }
  namespace = "voice." + path.removePrefix(":").replace(':', '.')
  compileSdk = VoiceVersions.compileSdk
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = VoiceVersions.javaCompileVersion
    targetCompatibility = VoiceVersions.javaCompileVersion
  }
  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.composeVersion
  }
}

dependencies {
  coreLibraryDesugaring(libs.desugar)
}
