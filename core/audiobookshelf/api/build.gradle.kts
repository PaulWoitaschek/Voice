plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.featureflag)
}
