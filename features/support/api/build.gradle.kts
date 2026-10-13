plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.data.api)
  api(projects.core.featureflag)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  api(projects.navigation)
}
