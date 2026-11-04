plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.audiobookshelf.api)
  implementation(projects.core.data.api)

  implementation(libs.coil)
  implementation(libs.navigation3.ui)
}
