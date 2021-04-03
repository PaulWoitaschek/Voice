import deps.Deps
import deps.Versions
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
    versionCode = Versions.versionCode
    versionName = Versions.versionName

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

  lintOptions {
    isCheckDependencies = true
    isIgnoreTestSources = true
    isWarningsAsErrors = true
  }

  packagingOptions {
    pickFirst("META-INF/atomicfu.kotlin_module")
    pickFirst("META-INF/core.kotlin_module")
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

  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.AndroidX.recyclerView)
  implementation(Deps.material)
  implementation(Deps.AndroidX.transitions)
  implementation(Deps.AndroidX.constraintLayout)
  implementation(Deps.AndroidX.mediaCompat)

  implementation(Deps.picasso)
  implementation(Deps.Kotlin.Serialization.core)

  implementation(Deps.MaterialDialog.core)
  implementation(Deps.MaterialDialog.input)
  implementation(Deps.materialCab)

  implementation(Deps.floatingActionButton)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  implementation(Deps.AndroidX.ktx)

  testImplementation(Deps.junit)
  testImplementation(Deps.truth)
  testImplementation(Deps.mockk)

  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)

  implementation(Deps.timber)

  implementation(Deps.ExoPlayer.core)
  implementation(Deps.ExoPlayer.flac) { isTransitive = false }

  implementation(Deps.Conductor.core)
  implementation(Deps.Conductor.transition)

  implementation(Deps.lifecycle)

  implementation(Deps.groupie)

  implementation(Deps.Prefs.android)
  testImplementation(Deps.Prefs.inMemory)

  implementation(Deps.tapTarget)
  testImplementation(Deps.AndroidX.Test.runner)
  testImplementation(Deps.AndroidX.Test.junit)
  testImplementation(Deps.AndroidX.Test.core)
  testImplementation(Deps.robolectric)
  testImplementation(Deps.Kotlin.coroutinesTest)

  androidTestImplementation(Deps.truth)
  androidTestImplementation(Deps.junit)
  androidTestImplementation(Deps.AndroidX.Test.runner)
  androidTestImplementation(Deps.AndroidX.Test.core)
  androidTestImplementation(Deps.AndroidX.Test.junit)
}

tasks.create("fdroid").dependsOn(":app:assembleOpensourceRelease")
