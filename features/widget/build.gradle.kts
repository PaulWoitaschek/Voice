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
  api(projects.core.ui)
  api(projects.core.initializer)
  api(projects.core.data.api)
  api(projects.core.playback)
  api(projects.core.sleeptimer.api)

  api(libs.glance.appwidget)
  implementation(libs.glance.material3)
  // Glance pulls in WorkManager 2.7.1, whose keep rules let R8 strip the InputMerger constructor,
  // so widget sessions never start in release builds
  runtimeOnly(libs.workManager)
}
