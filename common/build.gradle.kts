plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
}

android {
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(projects.ffmpeg)
  implementation(projects.strings)
  implementation(libs.appCompat)
  implementation(libs.coroutines.core)
  implementation(libs.dagger.core)
  implementation(libs.timber)
  implementation(libs.appCompat)
  implementation(libs.material)
  implementation(libs.bundles.compose)
  implementation(libs.prefs.core)
  api(libs.conductor.core)

  testImplementation(libs.truth)
  testImplementation(libs.junit)
}
