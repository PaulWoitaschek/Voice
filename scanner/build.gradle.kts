import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")
}

android {
  flavorDimensions("free")
  productFlavors {
    create("opensource") {
      dimension = "free"
    }
    create("proprietary") {
      dimension = "free"
    }
  }
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":data"))
  implementation(project(":common"))
  implementation(Deps.Prefs.core)
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Dagger.core)
  implementation(Deps.timber)
  implementation(Deps.picasso)
  implementation(Deps.Kotlin.Serialization.core)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}
