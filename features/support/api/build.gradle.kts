plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android{
  androidResources.enable=true
}
dependencies {
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.navigation)

  testImplementation(libs.molecule)
  testImplementation(libs.turbine)
}
