plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.featureflag)
  api(projects.core.datastore)
}
