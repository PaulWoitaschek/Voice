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
  implementation(projects.core.strings)
  implementation(projects.core.common)
  implementation(projects.core.ui)
  implementation(projects.core.initializer)
  implementation(projects.core.data.api)
  implementation(projects.core.playback)
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.logging.api)

  implementation(libs.glance.appwidget)
  implementation(libs.glance.material3)
  // Glance pulls in WorkManager 2.7.1, whose keep rules let R8 strip the InputMerger constructor,
  // so widget sessions never start in release builds
  implementation(libs.workManager)
  implementation(libs.materialKolor)
  implementation(libs.coil)
  implementation(libs.androidxCore)
}
