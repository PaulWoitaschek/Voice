plugins {
  id("com.android.library")
  id("kotlin-android")
}

dependencies {
  implementation(libs.picasso)
  implementation(libs.palette)
  implementation(libs.coroutines.core)
  implementation(libs.coroutines.android)
  implementation(libs.timber)
}
