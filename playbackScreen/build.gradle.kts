import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories = true
}

android {
  buildFeatures {
    viewBinding = true
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

  implementation(Deps.Dagger.core)
}
