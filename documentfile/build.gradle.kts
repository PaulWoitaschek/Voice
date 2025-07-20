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
  implementation(projects.common)
  implementation(libs.dagger.core)
  implementation(libs.androidxCore)
}
