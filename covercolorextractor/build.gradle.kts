import deps.Deps
import deps.Versions
import org.jetbrains.kotlin.gradle.dsl.Coroutines

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
    setSourceCompatibility(Versions.sourceCompatibility)
    setTargetCompatibility(Versions.targetCompatibility)
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

kotlin {
  experimental {
    coroutines = Coroutines.ENABLE
  }
}
