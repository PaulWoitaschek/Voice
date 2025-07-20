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
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.common)
  implementation(projects.datastore)
  api(libs.review)
  implementation(libs.lottie)
  implementation(libs.dagger.core)
  implementation(projects.remoteconfig.core)
}
