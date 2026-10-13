plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.navigation)
  api(projects.core.common)
  api(projects.core.data.api)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
}
