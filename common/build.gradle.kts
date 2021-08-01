plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
  generateDaggerFactoriesOnly.set(true)
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":strings"))
  implementation(libs.appCompat)
  implementation(libs.coroutines.core)
  implementation(libs.dagger.core)
  implementation(libs.timber)
  implementation(libs.appCompat)
  implementation(libs.material)
  api(libs.conductor.core)

  testImplementation(libs.truth)
  testImplementation(libs.junit)
}
