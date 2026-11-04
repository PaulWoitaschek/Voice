plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.initializer)
  implementation(projects.core.data.api)
  implementation(projects.core.playback.api)
  implementation(projects.core.sleeptimer.api)

  implementation(libs.glance.appwidget)
  implementation(libs.glance.material3)
  // Glance pulls in WorkManager 2.7.1, whose keep rules let R8 strip the InputMerger constructor,
  // so widget sessions never start in release builds
  implementation(libs.workManager)
  implementation(libs.materialKolor)
  implementation(libs.coil)
}
