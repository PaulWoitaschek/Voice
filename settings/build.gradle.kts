plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

android {
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.prefs)

  implementation(libs.timber)
  implementation(libs.coroutines.core)
  implementation(libs.picasso)
  implementation(libs.androidxCore)
  implementation(libs.prefs.core)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.constraintLayout)
  implementation(libs.material)

  implementation(libs.dagger.core)
  kapt(libs.dagger.compiler)

  implementation(libs.bundles.compose)
}
