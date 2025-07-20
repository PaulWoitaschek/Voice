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
  implementation(libs.dagger.core)
  api(libs.serialization.json)
  api(libs.datastore)
}
