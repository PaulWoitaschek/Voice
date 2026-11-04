plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.featureflag)
}
