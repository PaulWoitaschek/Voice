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
  }

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
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
}

dependencies {
  implementation(project(":common"))
  implementation(project(":core"))
  implementation(project(":data"))
  implementation(project(":crashreporting"))
  implementation(project(":prefs"))

  implementation(Deps.timber)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.AndroidX.mediaCompat)
  implementation(Deps.picasso)
  implementation(Deps.AndroidX.ktx)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  implementation(Deps.ExoPlayer.core)
  implementation(Deps.ExoPlayer.flac) { isTransitive = false }
  implementation(Deps.ExoPlayer.opus) { isTransitive = false }
}
