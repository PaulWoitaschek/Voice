import java.util.Properties

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-parcelize")
  id("kotlinx-serialization")
  id("kotlin-kapt")
  id("com.squareup.anvil")
}

android {

  defaultConfig {
    applicationId = "de.ph1b.audiobook"
    versionCode = 3060342
    versionName = "5.0.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
      abiFilters.clear()
      abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }
  }

  signingConfigs {
    create("release") {
      val props = Properties()
      var propsFile = File(rootDir, "signing/signing.properties")
      if (!propsFile.canRead()) {
        println("Use CI keystore.")
        propsFile = File(rootDir, "signing/ci/signing.properties")
      }
      props.load(propsFile.inputStream())
      storeFile = File(propsFile.parentFile, props["STORE_FILE"] as String)
      storePassword = props["STORE_PASSWORD"] as String
      keyAlias = props["KEY_ALIAS"] as String
      keyPassword = props["KEY_PASSWORD"] as String
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
      setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard.pro"))
    }
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }

  lint {
    isCheckDependencies = true
    isIgnoreTestSources = true
    isWarningsAsErrors = true
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
  implementation(project(":strings"))
  implementation(project(":common"))
  implementation(project(":data"))
  implementation(project(":covercolorextractor"))
  implementation(project(":playback"))
  implementation(project(":prefs"))
  implementation(project(":ffmpeg"))
  implementation(project(":scanner"))
  implementation(project(":playbackScreen"))
  implementation(project(":sleepTimer"))
  implementation(project(":loudness"))
  implementation(project(":settings"))

  implementation(libs.appCompat)
  implementation(libs.recyclerView)
  implementation(libs.material)
  implementation(libs.transitions)
  implementation(libs.constraintLayout)
  implementation(libs.media)

  implementation(libs.picasso)
  implementation(libs.serialization.json)

  implementation(libs.materialDialog.core)
  implementation(libs.materialDialog.input)
  implementation(libs.materialCab)

  implementation(libs.floatingActionButton)

  implementation(libs.dagger.core)
  kapt(libs.dagger.compiler)

  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.mockk)

  implementation(libs.coroutines.core)
  implementation(libs.coroutines.android)

  implementation(libs.timber)

  implementation(libs.exoPlayer.core)
  implementation(libs.exoPlayer.flac) { isTransitive = false }

  implementation(libs.conductor.core)
  implementation(libs.conductor.transition)

  implementation(libs.lifecycle)

  implementation(libs.groupie)

  implementation(libs.prefs.android)
  testImplementation(libs.prefs.inMemory)

  implementation(libs.tapTarget)
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

tasks.create("fdroid").dependsOn(":app:assembleOpensourceRelease")
