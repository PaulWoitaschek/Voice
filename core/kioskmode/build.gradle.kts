plugins {
  id("voice.library")
}

dependencies {
  api(projects.core.data.api)
  implementation(projects.core.featureflag)
}
