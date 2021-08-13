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
  implementation(projects.common)
  implementation(libs.appCompat)
  implementation(libs.timber)
  implementation(libs.coroutines.core)
  implementation(libs.coroutines.android)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)

  api(libs.room.runtime)
  kapt(libs.room.compiler)

  implementation(libs.dagger.core)

  testImplementation(libs.room.testing)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
}
