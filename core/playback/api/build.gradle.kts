plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.data.api)
  api(projects.core.datastore)
}
