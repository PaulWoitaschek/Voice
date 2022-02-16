plugins {
  id("voice-android-library")
}

dependencies {
  implementation(projects.logging.core)
  implementation(libs.appStartup)
  implementation(libs.firebase.crashlytics)
}
