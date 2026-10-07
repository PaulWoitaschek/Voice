plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  implementation(libs.billing)

  testImplementation(libs.turbine)
}
