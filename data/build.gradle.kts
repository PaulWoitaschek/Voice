import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("voice-android-library")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
  alias(libs.plugins.ksp)
}

anvil {
  generateDaggerFactories.set(true)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

android {

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

tasks.withType<KotlinCompile>().configureEach {
  // workaround for https://youtrack.jetbrains.com/issue/KT-38576
  usePreciseJavaTracking = false
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
  ksp(libs.room.compiler)

  implementation(libs.dagger.core)

  testImplementation(libs.room.testing)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.koTest.assert)
}
