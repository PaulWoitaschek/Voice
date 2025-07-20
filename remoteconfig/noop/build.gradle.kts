plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
  }
}

dependencies {
  implementation(projects.remoteconfig.core)
  implementation(projects.common)
  implementation(libs.dagger.core)
}
