plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.core.ui)
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.playback)
  implementation(projects.core.data.api)
  implementation(projects.core.documentfile)
  implementation(projects.navigation)
  implementation(projects.core.featureflag)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)

  testImplementation(libs.molecule)
}
