plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.features.support.api)

  implementation(projects.core.featureflag)
  implementation(projects.navigation)

  testImplementation(libs.turbine)
}
