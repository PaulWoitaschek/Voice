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

  lintOptions {
    isWarningsAsErrors = true
  }

  compileOptions {
    sourceCompatibility = Versions.sourceCompatibility
    targetCompatibility = Versions.targetCompatibility
  }
}
