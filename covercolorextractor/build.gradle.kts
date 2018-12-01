import deps.Deps
import deps.Versions

plugins {
  id("com.android.library")
  id("kotlin-android")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)
  }

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
  }
}

dependencies {
  implementation(Deps.picasso)
  implementation(Deps.Kotlin.std)
  implementation(Deps.AndroidX.palette)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.timber)
}
