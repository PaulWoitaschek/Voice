plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.datastore)
}
