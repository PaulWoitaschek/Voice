plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.playback.api)
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
}
