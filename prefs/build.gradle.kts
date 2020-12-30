import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
}

dependencies {
  implementation(Deps.Kotlin.coroutines)
}
