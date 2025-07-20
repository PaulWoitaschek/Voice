plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
    includeAnvil()
  }
}

dependencies {
  implementation(projects.remoteconfig.core)
  implementation(projects.common)
  implementation(libs.firebase.remoteconfig)
  implementation(libs.dagger.core)
}
