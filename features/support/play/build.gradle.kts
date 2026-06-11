plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  testImplementation(libs.turbine)
}
