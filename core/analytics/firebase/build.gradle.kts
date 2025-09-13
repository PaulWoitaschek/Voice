plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.analytics.api)
  implementation(libs.firebase.analytics)
}
