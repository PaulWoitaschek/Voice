plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.data)
}
