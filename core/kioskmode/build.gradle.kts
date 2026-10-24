plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.data.api)
  implementation(projects.core.featureflag)
}
