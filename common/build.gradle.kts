import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
}

dependencies {
  implementation(project(":ffmpeg"))
  implementation(project(":core"))
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Dagger.core)
  implementation(Deps.timber)
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.material)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}
