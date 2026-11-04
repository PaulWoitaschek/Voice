plugins {
  id("voice.library")
}

dependencies {
  api(projects.core.analytics.api)
  implementation(projects.core.initializer)
  implementation(libs.firebase.analytics)
}
