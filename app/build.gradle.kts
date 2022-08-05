@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
  id("voice.app")
  id("voice.compose")
  id("kotlin-parcelize")
  id("kotlin-kapt")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.playPublish)
}

val enableCrashlytics = project.hasProperty("enableCrashlytics")
if (enableCrashlytics) {
  pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
  pluginManager.apply(libs.plugins.googleServices.get().pluginId)
}

play {
  defaultToAppBundles.value(true)
  val trackProperty = providers.gradleProperty("voice.play-track")
  track.value(trackProperty)
  userFraction.value(
    trackProperty.map {
      if (it == "release") 0.01 else 1.0
    },
  )
  val serviceAccountJson = file("play_service_account.json")
  if (serviceAccountJson.exists()) {
    serviceAccountCredentials.set(serviceAccountJson)
  }
}

kapt {
  arguments {
    arg("dagger.fastInit", "enabled")
    arg("dagger.fullBindingGraphValidation", "ERROR")
  }
}


android {

  namespace = "voice.app"

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters.clear()
      abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }
  }

  signingConfigs {
    create("release") {
      val properties = Properties()
      val keyStoreName = if (providers.gradleProperty("voice.signing.play").get().toBoolean()) {
        "play"
      } else {
        "github"
      }
      val propertiesFile = rootProject.file("signing/$keyStoreName/signing.properties")
        .takeIf { it.canRead() }
        ?: rootProject.file("signing/ci/signing.properties")
      properties.load(propertiesFile.inputStream())
      storeFile = File(propertiesFile.parentFile, "signing.keystore")
      storePassword = properties["STORE_PASSWORD"] as String
      keyAlias = properties["KEY_ALIAS"] as String
      keyPassword = properties["KEY_PASSWORD"] as String
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
    }
    getByName("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
    }
    all {
      signingConfig = signingConfigs.getByName("release")
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.pro"))
    }
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }

  lint {
    checkDependencies = true
    ignoreTestSources = true
    warningsAsErrors = true
    lintConfig = rootProject.file("lint.xml")
  }

  packagingOptions {
    with(resources.pickFirsts) {
      add("META-INF/atomicfu.kotlin_module")
      add("META-INF/core.kotlin_module")
    }
  }

  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(projects.strings)
  implementation(projects.common)
  implementation(projects.data)
  implementation(projects.playback)
  implementation(projects.ffmpeg)
  implementation(projects.scanner)
  implementation(projects.playbackScreen)
  implementation(projects.sleepTimer)
  implementation(projects.settings)
  implementation(projects.folderPicker)
  implementation(projects.bookOverview)
  implementation(projects.migration)

  implementation(libs.appCompat)
  implementation(libs.recyclerView)
  implementation(libs.material)
  implementation(libs.transitions)
  implementation(libs.constraintLayout)
  implementation(libs.media)
  implementation(libs.datastore)
  implementation(libs.appStartup)

  implementation(libs.serialization.json)

  implementation(libs.materialDialog.core)
  implementation(libs.materialDialog.input)
  implementation(libs.materialCab)
  implementation(libs.coil)

  if (enableCrashlytics) {
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(projects.logging.crashlytics)
  }

  debugImplementation(projects.logging.debug)

  implementation(libs.dagger.core)
  kapt(libs.dagger.compiler)

  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.mockk)

  implementation(libs.exoPlayer.core)

  implementation(libs.conductor.core)
  implementation(libs.conductor.transition)

  implementation(libs.lifecycle)

  implementation(libs.groupie)

  implementation(libs.prefs.android)
  testImplementation(libs.prefs.inMemory)

  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.robolectric)
  testImplementation(libs.coroutines.test)

  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidX.test.runner)
  androidTestImplementation(libs.androidX.test.core)
  androidTestImplementation(libs.androidX.test.junit)
}
