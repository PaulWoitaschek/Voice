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
  implementation(libs.dagger.core)
  api(libs.serialization.json)
  api(libs.datastore)
}
