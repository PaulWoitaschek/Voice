plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.datastore)
  implementation(projects.core.analytics.api)

  implementation(libs.coil)
}
