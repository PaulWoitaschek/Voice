plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
    includeAnvil()
  }
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.sleepTimer)
  implementation(projects.documentfile)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)

  testImplementation(libs.molecule)

  implementation(libs.dagger.core)
}
