plugins {
  id("voice.library")
}

dependencies {
  api(projects.features.support.api)

  implementation(projects.navigation)
}
