import deps.Deps
import deps.Versions

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    javaCompileOptions {
      annotationProcessorOptions {
        arguments = mapOf("room.schemaLocation" to "$projectDir/schemas")
      }
    }
  }


  flavorDimensions("free")
  productFlavors {
    create("opensource") {
      setDimension("free")
    }
    create("proprietary") {
      setDimension("free")
    }
  }

  sourceSets {
    // debug as a workaround, see https://github.com/robolectric/robolectric/issues/3928
    getByName("debug").assets.srcDir(files("$projectDir/schemas"))
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":crashreporting"))
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.timber)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.rxJava)
  implementation(Deps.moshi)
  implementation(Deps.AndroidX.ktx)

  api(Deps.AndroidX.Room.runtime)
  implementation(Deps.AndroidX.Room.rxJava)
  kapt(Deps.AndroidX.Room.compiler)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  testImplementation(Deps.AndroidX.Room.testing)
  testImplementation(Deps.AndroidX.Test.core)
  testImplementation(Deps.AndroidX.Test.junit)
  testImplementation(Deps.AndroidX.Test.runner)
  testImplementation(Deps.junit)
  testImplementation(Deps.robolectric)
  testImplementation(Deps.truth)
}
