plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  testImplementation(projects.core.data.impl)
}
