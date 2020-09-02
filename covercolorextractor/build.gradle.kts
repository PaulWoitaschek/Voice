import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
}

dependencies {
  implementation(Deps.picasso)
  implementation(Deps.AndroidX.palette)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.timber)
}
