plugins {
  id("voice.library")
}

dependencies {
  api(projects.features.support.api)

  implementation(libs.billing)
  implementation(libs.datastore)

  implementation(projects.core.analytics.api)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.navigation)
}
