import deps.Deps

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlinx-serialization")
  id("kotlin-kapt")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
}

android {

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    javaCompileOptions {
      annotationProcessorOptions {
        argument("room.schemaLocation", "$projectDir/schemas")
      }
    }
  }

  sourceSets {
    named("test") {
      assets.srcDir(project.file("schemas"))
    }
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    animationsDisabled = true
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  implementation(project(":common"))
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.timber)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.AndroidX.ktx)
  implementation(Deps.Kotlin.Serialization.core)

  api(Deps.AndroidX.Room.runtime)
  kapt(Deps.AndroidX.Room.compiler)

  implementation(Deps.Dagger.core)

  testImplementation(Deps.AndroidX.Room.testing)
  testImplementation(Deps.AndroidX.Test.core)
  testImplementation(Deps.AndroidX.Test.junit)
  testImplementation(Deps.AndroidX.Test.runner)
  testImplementation(Deps.junit)
  testImplementation(Deps.robolectric)
  testImplementation(Deps.truth)
}
