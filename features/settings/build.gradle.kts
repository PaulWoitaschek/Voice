plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.remoteconfig.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.playback.api)
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.analytics.api)
  implementation(projects.core.documentfile)
  implementation(projects.core.audiobookshelf.api)
}
