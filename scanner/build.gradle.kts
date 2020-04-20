import deps.Deps
import deps.Versions

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)
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

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
  }
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":data"))
  implementation(project(":common"))
  implementation(Deps.Prefs.core)
  implementation(Deps.Kotlin.std)
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Dagger.core)
  implementation(Deps.timber)
  implementation(Deps.picasso)
  implementation(Deps.Kotlin.Serialization.runtime)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}
