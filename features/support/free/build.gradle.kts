plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.navigation)

  testImplementation(libs.turbine)
}
