plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
  generateDaggerFactoriesOnly.set(true)
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":data"))
  implementation(project(":common"))
  implementation(libs.prefs.core)
  implementation(libs.appCompat)
  implementation(libs.coroutines.core)
  implementation(libs.dagger.core)
  implementation(libs.timber)
  implementation(libs.picasso)
  implementation(libs.serialization.json)

  testImplementation(libs.truth)
  testImplementation(libs.junit)
}
