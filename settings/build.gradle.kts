import deps.Deps
import deps.composeImplementation

plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

android {
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = deps.Versions.compose
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":strings"))
  implementation(project(":playback"))
  implementation(project(":data"))
  implementation(project(":prefs"))

  implementation(Deps.timber)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.picasso)
  implementation(Deps.AndroidX.ktx)
  implementation(Deps.Prefs.core)
  implementation(Deps.MaterialDialog.core)
  implementation(Deps.AndroidX.ktx)
  implementation(Deps.AndroidX.constraintLayout)
  implementation(Deps.material)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  composeImplementation()
}
