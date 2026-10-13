plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  api(libs.billing)
  api(libs.datastore)

  api(projects.core.analytics.api)
  api(projects.core.data.api)
  api(projects.core.initializer)
  api(projects.navigation)
}
