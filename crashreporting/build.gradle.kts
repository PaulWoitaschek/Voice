import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
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
  add("proprietaryImplementation", Deps.crashlytics) {
    isTransitive = true
  }
  implementation(Deps.timber)
}
