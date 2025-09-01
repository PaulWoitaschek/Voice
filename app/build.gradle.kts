@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.gradle.internal.dsl.SigningConfig
import java.util.Properties

plugins {
  id("voice.app")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.playPublish)
}

fun includeProprietaryLibraries(): Boolean {
  val includeProprietaryLibraries = providers.gradleProperty("voice.includeProprietaryLibraries").get().toBooleanStrict()
  if (!includeProprietaryLibraries) {
    return false
  }
  return file("google-services.json").exists()
    .also { present ->
      if (!present) {
        logger.warn("Google Services JSON file not found, disabling proprietary libraries.")
      }
    }
}

if (includeProprietaryLibraries()) {
  pluginManager.apply(libs.plugins.googleServices.get().pluginId)
  pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
}

play {
  defaultToAppBundles.value(true)
  val serviceAccountJson = file("play_service_account.json")
  if (serviceAccountJson.exists()) {
    serviceAccountCredentials.set(serviceAccountJson)
  }
}

android {

  namespace = "voice.app"

  androidResources {
    generateLocaleConfig = true
  }

  dependenciesInfo {
    // disable the dependencies info in apks to allow reproducible builds
    // see https://github.com/PaulWoitaschek/Voice/discussions/2862#discussioncomment-13622836
    includeInApk = false
  }

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()

    testInstrumentationRunner = "voice.app.VoiceJUnitRunner"
  }

  fun createSigningConfig(name: String): SigningConfig {
    return signingConfigs.create(name) {
      val properties = Properties()
      val propertiesFile = rootProject.file("signing/$name/signing.properties")
        .takeIf { it.canRead() }
        ?: rootProject.file("signing/ci/signing.properties")
      properties.load(propertiesFile.inputStream())
      storeFile = File(propertiesFile.parentFile, "signing.keystore")
      storePassword = properties["STORE_PASSWORD"] as String
      keyAlias = properties["KEY_ALIAS"] as String
      keyPassword = properties["KEY_PASSWORD"] as String
    }
  }

  val playSigningConfig = createSigningConfig("play")
  val githubSigningConfig = createSigningConfig("github")

  val signingFlavor = "signing"
  flavorDimensions += signingFlavor
  productFlavors {
    register("github") {
      dimension = signingFlavor
      signingConfig = githubSigningConfig
    }
    register("play") {
      dimension = signingFlavor
      signingConfig = playSigningConfig
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
      setProguardFiles(
        listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard.pro",
        ),
      )
    }
  }

  testOptions {
    unitTests {
      isReturnDefaultValues = true
      isIncludeAndroidResources = true
    }
    animationsDisabled = true
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    managedDevices {
      allDevices.create<ManagedVirtualDevice>("voiceDevice") {
        device = "Pixel 9"
        apiLevel = 33
      }
    }
  }

  lint {
    checkDependencies = true
    ignoreTestSources = true
    warningsAsErrors = providers.gradleProperty("voice.warningsAsErrors").get().toBooleanStrict()
    lintConfig = rootProject.file("lint.xml")
  }

  packaging {
    with(resources.pickFirsts) {
      add("META-INF/atomicfu.kotlin_module")
      add("META-INF/core.kotlin_module")
    }
  }

  buildFeatures {
    buildConfig = true
  }
}

dependencies {
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.data.impl)
  implementation(projects.core.playback)
  implementation(projects.core.scanner)
  implementation(projects.core.initializer)
  implementation(projects.features.playbackScreen)
  implementation(projects.navigation)
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.sleeptimer.impl)
  implementation(projects.features.sleepTimer)
  implementation(projects.features.settings)
  implementation(projects.features.folderPicker)
  implementation(projects.features.bookOverview)
  implementation(projects.core.search)
  implementation(projects.features.cover)
  implementation(projects.core.documentfile)
  implementation(projects.features.onboarding)
  implementation(projects.features.bookmark)
  implementation(projects.features.widget)

  implementation(libs.appCompat)
  implementation(libs.material)
  implementation(libs.datastore)

  implementation(libs.navigation3.ui)

  implementation(libs.serialization.json)

  implementation(libs.coil)

  if (includeProprietaryLibraries()) {
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(projects.core.logging.crashlytics)
    implementation(projects.features.review.play)
    implementation(projects.core.remoteconfig.firebase)
  } else {
    implementation(projects.features.review.noop)
    implementation(projects.core.remoteconfig.noop)
  }
  implementation(projects.core.remoteconfig.core)

  debugImplementation(projects.core.logging.debug)

  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)

  implementation(libs.media3.exoplayer)
  implementation(libs.media3.session)

  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.robolectric)
  testImplementation(libs.coroutines.test)
  testImplementation(kotlin("reflect"))

  debugImplementation(libs.compose.ui.testManifest)

  androidTestImplementation(libs.androidX.test.espresso.core)
  androidTestImplementation(libs.androidX.test.runner)
  androidTestImplementation(libs.androidX.test.rules)
  androidTestImplementation(libs.androidX.test.junit)
  androidTestImplementation(libs.media3.testUtils.core)
  androidTestImplementation(libs.koTest.assert)
  androidTestImplementation(libs.androidX.test.services)
  androidTestImplementation(libs.coroutines.test)
  androidTestUtil(libs.androidX.test.orchestrator)
}
