import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
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
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
}
