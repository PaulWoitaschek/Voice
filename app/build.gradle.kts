@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ApplicationAndroidResources
import com.android.build.api.dsl.ManagedVirtualDevice
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

val useProprietaryLibraries = System.getenv("VOICE_USE_PROPRIETARY_LIBRARIES") == "true"
if (useProprietaryLibraries) {
  pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
  if (file("google-services.json").exists()) {
    pluginManager.apply(libs.plugins.googleServices.get().pluginId)
  }
}

play {
  defaultToAppBundles.value(true)
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

  androidResources {
    // since https://github.com/PaulWoitaschek/Voice/pull/2131 this cast is required.
    // this is very strange and should not be necessary
    (this as ApplicationAndroidResources).generateLocaleConfig = true
  }

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
      setProguardFiles(
        listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard.pro",
          // remove if retrofit > 2.9.0 is released https://github.com/square/retrofit/issues/3751
          "retrofit2.pro",
        ),
      )
    }
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    managedDevices {
      devices.create<ManagedVirtualDevice>("pixel5") {
        device = "Pixel 5"
        apiLevel = 31
      }
      devices.create<ManagedVirtualDevice>("nexus7") {
        device = "Nexus 7"
        apiLevel = 31
      }
      devices.create<ManagedVirtualDevice>("nexus10") {
        device = "Nexus 10"
        apiLevel = 31
      }
      groups.create("screenshotDevices") {
        targetDevices.addAll(devices.toList())
      }
    }
  }

  lint {
    checkDependencies = true
    ignoreTestSources = true
    warningsAsErrors = true
    lintConfig = rootProject.file("lint.xml")
  }

  packaging {
    with(resources.pickFirsts) {
      add("META-INF/atomicfu.kotlin_module")
      add("META-INF/core.kotlin_module")
    }
  }

  buildFeatures {
    viewBinding = true
    buildConfig = true
  }
}

dependencies {
  implementation(projects.strings)
  implementation(projects.datastore)
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
  implementation(projects.search)
  implementation(projects.cover)
  implementation(projects.documentfile)
  implementation(projects.onboarding)

  implementation(libs.appCompat)
  implementation(libs.recyclerView)
  implementation(libs.material)
  implementation(libs.constraintLayout)
  implementation(libs.datastore)
  implementation(libs.appStartup)

  implementation(libs.serialization.json)

  implementation(libs.materialDialog.core)
  implementation(libs.materialDialog.input)
  implementation(libs.coil)

  if (useProprietaryLibraries) {
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.remoteconfig)
    implementation(project(":logging:crashlytics"))
    implementation(project(":review:play"))
  } else {
    implementation(projects.review.noop)
  }

  debugImplementation(projects.logging.debug)

  implementation(libs.dagger.core)
  kapt(libs.dagger.compiler)

  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)

  implementation(libs.leakcanary.plumber)
  debugImplementation(libs.leakcanary.android)

  implementation(libs.media3.exoplayer)
  implementation(libs.media3.session)

  implementation(libs.conductor)

  implementation(libs.prefs.android)
  testImplementation(libs.prefs.inMemory)

  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.robolectric)
  testImplementation(libs.coroutines.test)

  androidTestUtil(libs.androidTest.orchestrator)
  androidTestUtil(libs.androidTest.services.testServices)
  androidTestImplementation(libs.androidTest.services.storage)

  androidTestImplementation(libs.androidTest.espresso)
  androidTestImplementation(libs.androidTest.rules)
  androidTestImplementation(libs.koTest.assert)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.compose.ui.test)
  debugImplementation(libs.compose.ui.testManifest)
  androidTestImplementation(libs.compose.ui.testJunit)
  androidTestImplementation(libs.androidX.test.runner)
  androidTestImplementation(libs.androidX.test.core)
  androidTestImplementation(libs.androidX.test.junit)
  androidTestImplementation(libs.uiautomator)
}
