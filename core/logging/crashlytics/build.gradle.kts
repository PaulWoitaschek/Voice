plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.initializer)
  implementation(libs.firebase.crashlytics)
}
