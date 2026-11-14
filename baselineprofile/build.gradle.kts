plugins {
  id("com.android.test")
  alias(libs.plugins.baselineProfile)
  id("voice.ktfmt")
}

android {
  namespace = "voice.baselineprofile"
  compileSdk = libs.versions.sdk.compile.get().toInt()
  targetProjectPath = ":app"

  defaultConfig {
    minSdk = libs.versions.sdk.min.get().toInt()
    targetSdk = libs.versions.sdk.target.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  flavorDimensions += "distribution"
  productFlavors { register("play") { dimension = "distribution" } }

  compileOptions {
    val bytecode = JavaVersion.toVersion(libs.versions.jvm.bytecode.get())
    sourceCompatibility = bytecode
    targetCompatibility = bytecode
  }

  testOptions.managedDevices.localDevices.register("baselineProfileDevice") {
    device = "Pixel 6"
    apiLevel = 34
    systemImageSource = "aosp"
  }
}

kotlin {
  jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

baselineProfile {
  managedDevices += "baselineProfileDevice"
  // The test installs its own build of the app, so it must never run on a listener's phone.
  useConnectedDevices = false
}

dependencies {
  implementation(libs.androidX.test.junit)
  implementation(libs.uiautomator)
  implementation(libs.benchmark.macro)
}
