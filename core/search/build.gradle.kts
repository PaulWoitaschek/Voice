plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.data.api)
  testImplementation(projects.core.data.impl)
  testImplementation(libs.room.runtime)
}
