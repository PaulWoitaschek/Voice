plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.featureflag)
}
