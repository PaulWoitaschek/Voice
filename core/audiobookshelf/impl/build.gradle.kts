plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.audiobookshelf.api)
  implementation(projects.core.analytics.api)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.playback.api)
  implementation(projects.core.strings)

  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.datasource.okhttp)

  testImplementation(libs.okhttp.mockwebserver)
}
