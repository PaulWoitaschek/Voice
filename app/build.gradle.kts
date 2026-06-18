@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
  id("voice.app")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.googleServices) apply false
}

val playGoogleServicesJson = layout.projectDirectory.file("src/play/google-services.json")
if (playGoogleServicesJson.asFile.canRead()) {
  pluginManager.apply(libs.plugins.googleServices.get().pluginId)
  pluginManager.apply(libs.plugins.crashlytics.get().pluginId)
}

android {

  namespace = "voice.app"

  dependenciesInfo {
    // disable the dependencies info in apks to allow reproducible builds
    // see https://github.com/PaulWoitaschek/Voice/discussions/2862#discussioncomment-13622836
    includeInApk = false
  }

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    versionName = providers.gradleProperty("voice.versionName").orNull ?: "1.0.0"
    versionCode = providers.gradleProperty("voice.versionCode").orNull?.toInt() ?: Int.MAX_VALUE

    testInstrumentationRunner = "voice.app.VoiceJUnitRunner"
  }

  val distributionFlavor = "distribution"
  flavorDimensions += distributionFlavor
  productFlavors {
    register("free") {
      dimension = distributionFlavor
      buildConfigField(type = "Boolean", name = "INCLUDE_ANALYTICS", value = "false")
      buildConfigField(type = "Boolean", name = "SUPPORT_DEVELOPMENT_INCLUDED", value = "true")
      pluginManager.withPlugin(libs.plugins.crashlytics.get().pluginId) {
        extensions.configure<CrashlyticsExtension>("firebaseCrashlytics") {
          mappingFileUploadEnabled = false
        }
      }
    }
    register("play") {
      dimension = distributionFlavor
      buildConfigField(type = "Boolean", name = "INCLUDE_ANALYTICS", value = "true")
      buildConfigField(type = "Boolean", name = "SUPPORT_DEVELOPMENT_INCLUDED", value = "false")
    }
  }

  val signingPropertiesFile = layout.projectDirectory.file("../signing/signing.properties").asFile
  val signingKeystoreFile = layout.projectDirectory.file("../signing/signing.keystore").asFile
  val appSigningConfig = if (signingPropertiesFile.isFile) {
    val signingProperties = Properties().apply {
      signingPropertiesFile.inputStream().use(::load)
    }
    signingConfigs.create("signing") {
      storeFile = signingKeystoreFile
      storePassword = signingProperties.getProperty("STORE_PASSWORD")
      keyAlias = signingProperties.getProperty("KEY_ALIAS")
      keyPassword = signingProperties.getProperty("KEY_PASSWORD")
    }
  } else {
    null
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
    }
    getByName("debug") {
      isMinifyEnabled = false
    }
    all {
      if (appSigningConfig != null) {
        signingConfig = appSigningConfig
      }
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
      allDevices.create("voiceDevice", ManagedVirtualDevice::class.java) {
        device = "Pixel 9"
        apiLevel = 33
      }
    }
  }

  lint {
    checkDependencies = true
    ignoreTestSources = true
    checkReleaseBuilds = false
    warningsAsErrors = providers.gradleProperty("voice.warningsAsErrors").get().toBooleanStrict()
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

val validatePlayGoogleServices by tasks.registering {
  val playGoogleServicesJsonPath = playGoogleServicesJson.asFile.absolutePath

  doLast {
    check(File(playGoogleServicesJsonPath).canRead()) {
      "app/src/play/google-services.json is required for Play builds. " +
        "Use the free variant for F-Droid/GitHub builds."
    }
  }
}

tasks.matching { it.name in setOf("prePlayDebugBuild", "prePlayReleaseBuild") }.configureEach {
  dependsOn(validatePlayGoogleServices)
}

tasks.matching {
  val isPlayTask = it.name.contains("Play")
  !isPlayTask && (
    it.name.startsWith("process") && it.name.endsWith("GoogleServices") ||
      it.name.startsWith("injectCrashlytics")
    )
}.configureEach {
  enabled = false
}

dependencies {
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.data.impl)
  implementation(projects.core.playback)
  implementation(projects.core.scanner)
  implementation(projects.core.featureflag)
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
  implementation(libs.lifecycle.compose)
  implementation(libs.datastore)

  implementation(libs.navigation3.ui)

  implementation(libs.serialization.json)

  implementation(libs.coil)

  add("playImplementation", libs.firebase.crashlytics)
  add("playImplementation", libs.firebase.analytics)
  add("playImplementation", projects.core.logging.crashlytics)
  add("playImplementation", projects.features.review.play)
  add("playImplementation", projects.features.support.play)
  add("freeImplementation", projects.features.review.noop)
  add("freeImplementation", projects.features.support.free)

  implementation(projects.core.remoteconfig.api)
  add("playImplementation", projects.core.remoteconfig.firebase)
  add("freeImplementation", projects.core.remoteconfig.noop)

  implementation(projects.core.analytics.api)
  add("playImplementation", projects.core.analytics.firebase)
  add("freeImplementation", projects.core.analytics.noop)

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

  androidTestImplementation(platform(libs.compose.bom))
  androidTestImplementation(libs.androidX.test.runner)
  androidTestImplementation(libs.androidX.test.rules)
  androidTestImplementation(libs.androidX.test.junit)
  androidTestImplementation(libs.media3.testUtils.core)
  androidTestImplementation(libs.kotlin.testJunit)
  androidTestImplementation(libs.androidX.test.services)
  androidTestImplementation(libs.compose.ui.testJunit)
  androidTestImplementation(libs.coroutines.test)
  androidTestUtil(libs.androidX.test.orchestrator)
}
