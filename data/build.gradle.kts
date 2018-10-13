import deps.Deps
import deps.Versions
import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {

  compileSdkVersion(Versions.compileSdk)

  defaultConfig {
    minSdkVersion(Versions.minSdk)
    targetSdkVersion(Versions.targetSdk)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    javaCompileOptions {
      annotationProcessorOptions {
        arguments = mapOf("room.schemaLocation" to "$projectDir/schemas")
      }
    }
  }

  sourceSets {
    getByName("androidTest").assets.srcDir(files("$projectDir/schemas"))
  }

  compileOptions {
    setSourceCompatibility(Versions.sourceCompatibility)
    setTargetCompatibility(Versions.targetCompatibility)
  }
}

dependencies {
  implementation(project(":common"))
  implementation(Deps.AndroidX.appCompat)
  implementation(Deps.timber)
  implementation(Deps.Kotlin.coroutines)
  implementation(Deps.Kotlin.coroutinesAndroid)
  implementation(Deps.rxJava)
  implementation(Deps.moshi)
  implementation(Deps.AndroidX.ktx)

  api(Deps.AndroidX.Room.runtime)
  implementation(Deps.AndroidX.Room.rxJava)
  kapt(Deps.AndroidX.Room.compiler)
  androidTestImplementation(Deps.AndroidX.Room.testing)

  implementation(Deps.Dagger.core)
  kapt(Deps.Dagger.compiler)

  androidTestImplementation(Deps.AndroidX.Test.runner)
  androidTestImplementation(Deps.truth)

  testImplementation(Deps.truth)
  testImplementation(Deps.junit)
}


kotlin {
  experimental {
    coroutines = Coroutines.ENABLE
  }
}
