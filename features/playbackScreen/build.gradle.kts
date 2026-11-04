plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.playback.api)
  implementation(projects.core.audiobookshelf.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.sleeptimer.api)

  implementation(libs.coil)
  implementation(libs.navigation3.ui)
}
