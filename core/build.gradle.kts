import deps.Versions

plugins {
  id("com.android.library")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)
  }

  compileOptions {
    setSourceCompatibility(Versions.sourceCompatibility)
    setTargetCompatibility(Versions.targetCompatibility)
  }
}
