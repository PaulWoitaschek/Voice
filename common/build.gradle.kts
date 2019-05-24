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
  implementation(Deps.Kotlin.std)
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.rxJava)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}
