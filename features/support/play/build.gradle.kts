plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  implementation(libs.billing)
  implementation(libs.datastore)

  implementation(projects.core.analytics.api)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.logging.api)
  implementation(projects.navigation)

  testImplementation(libs.turbine)
}
