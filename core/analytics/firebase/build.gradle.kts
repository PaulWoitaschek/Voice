plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.analytics.api)
  implementation(projects.core.initializer)
  implementation(projects.core.data.api)
  implementation(libs.firebase.analytics)
}
