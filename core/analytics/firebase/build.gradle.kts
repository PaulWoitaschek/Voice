plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.analytics.api)
  api(projects.core.initializer)
  api(projects.core.data.api)
  api(libs.firebase.analytics)
}
