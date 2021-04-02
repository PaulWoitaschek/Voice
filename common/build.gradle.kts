import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories = true
  generateDaggerFactoriesOnly = true
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":strings"))
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Dagger.core)
  implementation(Deps.timber)
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.material)
  api(Deps.Conductor.core)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}
