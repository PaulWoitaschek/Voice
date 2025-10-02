plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android {
  androidResources.enable = true
}

dependencies {
  api(libs.datastore)
  implementation(libs.material)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.strings)
  implementation(libs.lifecycle.viewmodel.compose)
}
