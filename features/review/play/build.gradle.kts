plugins {
  id("voice.feature")
}

dependencies {
  implementation(libs.coil)

  implementation(projects.core.analytics.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.playback.api)
  implementation(projects.core.featureflag)
}
